/*
 * Copyright (C) 2026 Briiqn
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.briiqn.libdeflate;

import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.ref.Cleaner;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/*
 * Why was this made?
 * The JNI wrappers of libdeflate were very heap heavy and inefficient/old when we are compressing many many packets per second from many clients
 * And they mostly just sucked to use and aren't as straight forward as basically downcalling the symbols to java.
 * ALSO ALSO, I dont want to maintain a JNI bindings library
 * */
public class LibDeflate {

  private static final Linker LINKER = Linker.nativeLinker();
  private static final Arena LIBRARY_ARENA = Arena.ofShared();
  private static final SymbolLookup LOOKUP = load();
  private static final MethodHandle ALLOC_COMPRESSOR = lookup("libdeflate_alloc_compressor",
      FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
  private static final MethodHandle FREE_COMPRESSOR = lookup("libdeflate_free_compressor",
      FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
  private static final MethodHandle ALLOC_DECOMPRESSOR = lookup("libdeflate_alloc_decompressor",
      FunctionDescriptor.of(ValueLayout.ADDRESS));
  private static final ThreadLocal<ThreadState> THREAD_STATE = ThreadLocal.withInitial(
      ThreadState::new);
  private static final MethodHandle FREE_DECOMPRESSOR = lookup("libdeflate_free_decompressor",
      FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
  private static final MethodHandle ZLIB_COMPRESS_BOUND = lookup("libdeflate_zlib_compress_bound",
      FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
  private static final MethodHandle ZLIB_COMPRESS = lookup("libdeflate_zlib_compress",
      FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
          ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
  private static final MethodHandle ZLIB_DECOMPRESS = lookup("libdeflate_zlib_decompress",
      FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
          ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG,
          ValueLayout.ADDRESS));
  private static final MethodHandle DEFLATE_COMPRESS_BOUND = lookup(
      "libdeflate_deflate_compress_bound",
      FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
  private static final MethodHandle DEFLATE_COMPRESS = lookup("libdeflate_deflate_compress",
      FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
          ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
  private static final MethodHandle DEFLATE_DECOMPRESS = lookup("libdeflate_deflate_decompress",
      FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
          ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG,
          ValueLayout.ADDRESS));
  private static final Cleaner CLEANER = Cleaner.create();
  private static final int INITIAL_SCRATCH = 2 * 1024 * 1024;

  private static SymbolLookup load() {
    String os = System.getProperty("os.name").toLowerCase();
    String filename = os.contains("win") ? "libdeflate.dll" :
        os.contains("mac") ? "libdeflate.dylib" : "libdeflate.so";

    try (InputStream is = LibDeflate.class.getResourceAsStream("/" + filename)) {
      if (is == null) {
        throw new RuntimeException("Could not find " + filename);
      }
      Path tempLib = Files.createTempFile("libdeflate_", "_" + filename);
      tempLib.toFile().deleteOnExit();
      Files.copy(is, tempLib, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      return SymbolLookup.libraryLookup(tempLib, LIBRARY_ARENA);
    } catch (IOException e) {
      throw new RuntimeException("Failed to extract native library", e);
    }
  }

  private static MethodHandle lookup(String name, FunctionDescriptor desc) {
    return LOOKUP.find(name).map(s -> LINKER.downcallHandle(s, desc))
        .orElseThrow(() -> new NoSuchMethodError(name));
  }

  public static byte[] compress(byte[] input, int level) {
    try {
      ThreadState state = THREAD_STATE.get();
      MemorySegment compressor = state.getCompressor(level);

      long maxBound = (long) ZLIB_COMPRESS_BOUND.invokeExact(compressor, (long) input.length);

      MemorySegment inSeg = state.in(input.length);
      MemorySegment outSeg = state.out(maxBound);
      MemorySegment.copy(MemorySegment.ofArray(input), 0, inSeg, 0, input.length);

      long compressedSize = (long) ZLIB_COMPRESS.invokeExact(
          compressor, inSeg, (long) input.length, outSeg, maxBound);

      if (compressedSize == 0) {
        throw new RuntimeException("Compression failed");
      }

      return outSeg.asSlice(0, compressedSize).toArray(ValueLayout.JAVA_BYTE);
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  public static byte[] decompress(byte[] compressedInput, int expectedSize) {
    try {
      ThreadState state = THREAD_STATE.get();
      long totalOut = ValueLayout.JAVA_LONG.byteSize() + expectedSize;

      MemorySegment inSeg = state.in(compressedInput.length);
      MemorySegment combinedOut = state.out(totalOut);

      MemorySegment actualSizePtr = combinedOut.asSlice(0, ValueLayout.JAVA_LONG.byteSize());
      MemorySegment outPayload = combinedOut.asSlice(ValueLayout.JAVA_LONG.byteSize(),
          expectedSize);

      MemorySegment.copy(MemorySegment.ofArray(compressedInput), 0, inSeg, 0,
          compressedInput.length);

      int result = (int) ZLIB_DECOMPRESS.invokeExact(
          state.decompressor,
          inSeg, (long) compressedInput.length,
          outPayload, (long) expectedSize,
          actualSizePtr);

      if (result != 0) {
        throw new RuntimeException("Decompression failed: " + result);
      }

      long actual = actualSizePtr.get(ValueLayout.JAVA_LONG, 0);
      return outPayload.asSlice(0, actual).toArray(ValueLayout.JAVA_BYTE);
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  public static byte[] compressDeflate(byte[] input, int level) {
    try {
      ThreadState state = THREAD_STATE.get();
      MemorySegment compressor = state.getCompressor(level);

      long maxBound = (long) DEFLATE_COMPRESS_BOUND.invokeExact(compressor, (long) input.length);

      MemorySegment inSeg = state.in(input.length);
      MemorySegment outSeg = state.out(maxBound);
      MemorySegment.copy(MemorySegment.ofArray(input), 0, inSeg, 0, input.length);

      long compressedSize = (long) DEFLATE_COMPRESS.invokeExact(
          compressor, inSeg, (long) input.length, outSeg, maxBound);

      if (compressedSize == 0) {
        throw new RuntimeException("Deflate compression failed");
      }

      return outSeg.asSlice(0, compressedSize).toArray(ValueLayout.JAVA_BYTE);
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  public static byte[] decompressDeflate(byte[] compressedInput, int expectedSize) {
    try {
      ThreadState state = THREAD_STATE.get();
      long totalOut = ValueLayout.JAVA_LONG.byteSize() + expectedSize;

      MemorySegment inSeg = state.in(compressedInput.length);
      MemorySegment combinedOut = state.out(totalOut);

      MemorySegment actualSizePtr = combinedOut.asSlice(0, ValueLayout.JAVA_LONG.byteSize());
      MemorySegment outPayload = combinedOut.asSlice(ValueLayout.JAVA_LONG.byteSize(),
          expectedSize);

      MemorySegment.copy(MemorySegment.ofArray(compressedInput), 0, inSeg, 0,
          compressedInput.length);

      int result = (int) DEFLATE_DECOMPRESS.invokeExact(
          state.decompressor,
          inSeg, (long) compressedInput.length,
          outPayload, (long) expectedSize,
          actualSizePtr);

      if (result != 0) {
        throw new RuntimeException("Deflate decompression failed: " + result);
      }

      long actual = actualSizePtr.get(ValueLayout.JAVA_LONG, 0);
      return outPayload.asSlice(0, actual).toArray(ValueLayout.JAVA_BYTE);
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  private static final class ThreadState {

    Arena arena;
    MemorySegment inBuf;
    MemorySegment outBuf;

    Map<Integer, MemorySegment> compressors = new HashMap<>();
    MemorySegment decompressor;

    ThreadState() {
      arena = Arena.ofConfined();
      inBuf = arena.allocate(INITIAL_SCRATCH);
      outBuf = arena.allocate(INITIAL_SCRATCH);

      try {
        decompressor = (MemorySegment) ALLOC_DECOMPRESSOR.invokeExact();
        CLEANER.register(this, new NativeFreer(decompressor, false));
      } catch (Throwable t) {
        throw new RuntimeException(t);
      }
    }

    MemorySegment getCompressor(int level) throws Throwable {
      MemorySegment compressor = compressors.get(level);
      if (compressor == null) {
        compressor = (MemorySegment) ALLOC_COMPRESSOR.invokeExact(level);
        compressors.put(level, compressor);
        CLEANER.register(this, new NativeFreer(compressor, true));
      }
      return compressor;
    }

    MemorySegment in(long required) {
      if (required > inBuf.byteSize()) {
        regrow(Math.max(required, inBuf.byteSize() * 2L), outBuf.byteSize());
      }
      return inBuf;
    }

    MemorySegment out(long required) {
      if (required > outBuf.byteSize()) {
        regrow(inBuf.byteSize(), Math.max(required, outBuf.byteSize() * 2L));
      }
      return outBuf;
    }

    private void regrow(long newInSize, long newOutSize) {
      arena.close();
      arena = Arena.ofConfined();
      inBuf = arena.allocate(newInSize);
      outBuf = arena.allocate(newOutSize);
    }
  }

  private record NativeFreer(MemorySegment address, boolean isCompressor) implements Runnable {

    @Override
    public void run() {
      try {
        if (isCompressor) {
          FREE_COMPRESSOR.invokeExact(address);
        } else {
          FREE_DECOMPRESSOR.invokeExact(address);
        }
      } catch (Throwable ignored) {
      }
    }
  }
}