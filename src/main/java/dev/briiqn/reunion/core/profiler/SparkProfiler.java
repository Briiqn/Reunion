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

package dev.briiqn.reunion.core.profiler;

import dev.briiqn.reunion.api.annotation.ReunionPlugin;
import dev.briiqn.reunion.api.plugin.Plugin;
import dev.briiqn.reunion.api.plugin.PluginDescription;
import dev.briiqn.reunion.api.plugin.PluginManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.zip.GZIPOutputStream;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import me.lucko.spark.proto.SparkProtos.CommandSenderMetadata;
import me.lucko.spark.proto.SparkProtos.PlatformMetadata;
import me.lucko.spark.proto.SparkProtos.PlatformStatistics;
import me.lucko.spark.proto.SparkProtos.PluginOrModMetadata;
import me.lucko.spark.proto.SparkProtos.SystemStatistics;
import me.lucko.spark.proto.SparkSamplerProtos.SamplerData;
import me.lucko.spark.proto.SparkSamplerProtos.SamplerMetadata;
import me.lucko.spark.proto.SparkSamplerProtos.StackTraceNode;
import me.lucko.spark.proto.SparkSamplerProtos.ThreadNode;
import one.profiler.AsyncProfiler;

@Log4j2
public class SparkProfiler {

  private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
  private static final String BYTEBIN_URL = "https://spark-usercontent.lucko.me/post";
  private static final String VIEWER_URL = "https://spark.lucko.me/";
  private static final String CONTENT_TYPE = "application/x-spark-sampler";
  private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase()
      .contains("win");

  private final AsyncProfiler asyncProfiler;
  private final int intervalMs;
  private final PluginManager pluginManager;
  private final Map<String, Node> fallbackThreads = new ConcurrentHashMap<>();
  private Mode mode;
  private long startTime;
  @Getter
  private volatile boolean running;
  private ScheduledExecutorService fallbackExecutor;

  public SparkProfiler(int intervalMs, PluginManager pluginManager) {
    this.intervalMs = intervalMs;
    this.pluginManager = pluginManager;
    this.asyncProfiler = IS_WINDOWS ? null : tryLoadAsyncProfiler();
    if (IS_WINDOWS) {
      log.warn(
          "async-profiler is not supported on Windows using JVM sampler");
    } else if (asyncProfiler == null) {
      log.warn("async-profiler native lib not found using JVM sampler");
    }
  }

  public SparkProfiler(int intervalMs) {
    this(intervalMs, null);
  }

  private AsyncProfiler tryLoadAsyncProfiler() {
    String lib = System.getProperty("os.name").toLowerCase().contains("mac")
        ? "libasyncProfiler.dylib" : "libasyncProfiler.so";
    try (InputStream is = SparkProfiler.class.getResourceAsStream("/" + lib)) {
      if (is != null) {
        Path tmp = Files.createTempFile("async-profiler_", "_" + lib);
        tmp.toFile().deleteOnExit();
        Files.copy(is, tmp, StandardCopyOption.REPLACE_EXISTING);
        return AsyncProfiler.getInstance(tmp.toString());
      }
    } catch (Exception ignored) {
    }
    try {
      return AsyncProfiler.getInstance();
    } catch (Exception e) {
      return null;
    }
  }

  public void start(Mode mode) throws Exception {
    this.mode = mode;
    this.startTime = System.currentTimeMillis();
    this.running = true;

    if (asyncProfiler != null) {
      String event = mode == Mode.ALLOC ? "alloc" : "cpu";
      long interval = mode == Mode.ALLOC ? 524288L : intervalMs * 1000000L;
      asyncProfiler.execute("start,event=" + event + ",interval=" + interval);
    } else {
      if (mode == Mode.ALLOC) {
        log.warn(
            "Alloc mode is not supported without async-profiler  falling back to CPU sampling.");
        this.mode = Mode.CPU;
      }
      startFallbackSampler();
    }
  }

  private void startFallbackSampler() {
    fallbackThreads.clear();
    fallbackExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "reunion-profiler-sampler");
      t.setDaemon(true);
      return t;
    });
    fallbackExecutor.scheduleAtFixedRate(this::sampleJvm, 0, intervalMs, TimeUnit.MILLISECONDS);
  }

  private void sampleJvm() {
    for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
      StackTraceElement[] stack = entry.getValue();
      if (stack.length == 0) {
        continue;
      }
      String name = entry.getKey().getName();
      Node root = fallbackThreads.computeIfAbsent(name, k -> new Node("", ""));
      root.time += intervalMs;
      Node current = root;
      for (int i = stack.length - 1; i >= 0; i--) {
        StackTraceElement e = stack[i];
        Node child = current.child(e.getClassName(), e.getMethodName());
        child.time += intervalMs;
        current = child;
      }
    }
  }

  public Path stop() throws Exception {
    byte[] proto = buildProto();
    Files.createDirectories(Path.of("profiles"));
    Path out = Path.of("profiles", "profile_" + LocalDateTime.now().format(FMT) + ".sparkprofile");
    Files.write(out, proto);
    return out;
  }

  public String stopAndUpload() throws Exception {
    byte[] proto = buildProto();
    Files.createDirectories(Path.of("profiles"));
    Path out = Path.of("profiles", "profile_" + LocalDateTime.now().format(FMT) + ".sparkprofile");
    Files.write(out, proto);
    return upload(proto);
  }

  private byte[] buildProto() throws Exception {
    running = false;
    long endTime = System.currentTimeMillis();
    Map<String, Node> threads;
    if (asyncProfiler != null) {
      String collapsed = asyncProfiler.execute("stop,collapsed,threads");
      threads = parseCollapsed(collapsed);
    } else {
      fallbackExecutor.shutdown();
      threads = fallbackThreads;
    }
    return encodeSamplerData(threads, startTime, endTime);
  }

  private Map<String, Node> parseCollapsed(String collapsed) {
    Map<String, Node> threads = new LinkedHashMap<>();
    for (String line : collapsed.split("\n")) {
      line = line.strip();
      if (line.isEmpty()) {
        continue;
      }
      int lastSpace = line.lastIndexOf(' ');
      if (lastSpace < 0) {
        continue;
      }
      double count;
      try {
        count = Double.parseDouble(line.substring(lastSpace + 1).trim());
      } catch (NumberFormatException e) {
        continue;
      }
      String[] frames = line.substring(0, lastSpace).split(";");
      if (frames.length == 0) {
        continue;
      }
      String threadName = frames[frames.length - 1];
      if (threadName.startsWith("[") && threadName.endsWith("]")) {
        threadName = threadName.substring(1, threadName.length() - 1);
      }
      Node root = threads.computeIfAbsent(threadName, k -> new Node("", ""));
      root.time += count;
      Node current = root;
      for (int i = 0; i < frames.length - 1; i++) {
        String frame = frames[i];
        String cn, mn;
        int dot = frame.lastIndexOf('.');
        if (dot > 0) {
          cn = frame.substring(0, dot).replace('/', '.');
          mn = frame.substring(dot + 1);
        } else {
          cn = frame.replace('/', '.');
          mn = "";
        }
        Node child = current.child(cn, mn);
        child.time += count;
        current = child;
      }
    }
    return threads;
  }

  private String upload(byte[] data) throws IOException, InterruptedException {
    byte[] compressed = gzip(data);
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(BYTEBIN_URL))
        .header("Content-Type", CONTENT_TYPE)
        .header("Content-Encoding", "gzip")
        .header("User-Agent", "Reunion/1.0")
        .POST(HttpRequest.BodyPublishers.ofByteArray(compressed))
        .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() != 201 && response.statusCode() != 200) {
      throw new IOException(
          "Upload failed: HTTP " + response.statusCode() + "  " + response.body());
    }
    String location = response.headers().firstValue("location")
        .orElseThrow(() -> new IOException("No location header in response"));
    String key = location.startsWith("/") ? location.substring(1) : location;
    return VIEWER_URL + key;
  }

  private byte[] gzip(byte[] data) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (GZIPOutputStream gz = new GZIPOutputStream(out)) {
      gz.write(data);
    }
    return out.toByteArray();
  }

  private PluginSources buildPluginSources() {
    Map<String, String> classSources = new LinkedHashMap<>();
    Map<String, PluginOrModMetadata> pluginMeta = new LinkedHashMap<>();

    if (pluginManager == null) {
      return new PluginSources(classSources, pluginMeta);
    }

    for (Plugin plugin : pluginManager.plugins()) {
      ReunionPlugin ann = plugin.getClass().getAnnotation(ReunionPlugin.class);
      if (ann == null) {
        continue;
      }

      PluginDescription desc = pluginManager.description(ann.id()).orElse(null);
      if (desc == null) {
        continue;
      }

      String author = desc.authors().isEmpty() ? "" : String.join(", ", desc.authors());

      pluginMeta.put(desc.id(), PluginOrModMetadata.newBuilder()
          .setName(desc.name())
          .setVersion(desc.version())
          .setAuthor(author)
          .setDescription(desc.description())
          .build());

      try {
        URL location = plugin.getClass().getProtectionDomain().getCodeSource().getLocation();
        URI uri = location.toURI();
        try (JarFile jar = new JarFile(Path.of(uri).toFile())) {
          jar.stream()
              .filter(e -> e.getName().endsWith(".class"))
              .map(e -> e.getName().replace('/', '.').replace(".class", ""))
              .filter(cn -> !cn.contains("$"))
              .forEach(cn -> classSources.put(cn, desc.id()));
        }
      } catch (Exception e) {
        log.warn("Failed to scan classes for plugin {}: {}", desc.id(), e.getMessage());
      }
    }

    return new PluginSources(classSources, pluginMeta);
  }

  private byte[] encodeSamplerData(Map<String, Node> threads, long startTime, long endTime) {
    PluginSources sources = buildPluginSources();

    SamplerData.Builder builder = SamplerData.newBuilder()
        .setMetadata(buildSamplerMetadata(startTime, endTime, sources.pluginMeta()))
        .putAllClassSources(sources.classSources());

    for (Map.Entry<String, Node> e : threads.entrySet()) {
      builder.addThreads(buildThreadNode(e.getKey(), e.getValue()));
    }

    return builder.build().toByteArray();
  }

  private SamplerMetadata buildSamplerMetadata(long startTime, long endTime,
      Map<String, PluginOrModMetadata> pluginMeta) {
    return SamplerMetadata.newBuilder()
        .setCreator(CommandSenderMetadata.newBuilder()
            .setName("Console")
            .build())
        .setStartTime(startTime)
        .setInterval(intervalMs * 1000)
        .setPlatformMetadata(PlatformMetadata.newBuilder()
            .setType(PlatformMetadata.Type.APPLICATION)
            .setName("Reunion")
            .setVersion("1.0")
            .build())
        .setPlatformStatistics(buildPlatformStatistics())
        .setSystemStatistics(buildSystemStatistics())
        .setEndTime(endTime)
        .setSamplerMode(mode == Mode.ALLOC
            ? SamplerMetadata.SamplerMode.ALLOCATION
            : SamplerMetadata.SamplerMode.EXECUTION)
        .setSamplerEngine(asyncProfiler != null
            ? SamplerMetadata.SamplerEngine.ASYNC
            : SamplerMetadata.SamplerEngine.JAVA)
        .putAllSources(pluginMeta)
        .build();
  }

  private PlatformStatistics buildPlatformStatistics() {
    MemoryMXBean mem = ManagementFactory.getMemoryMXBean();

    PlatformStatistics.Memory.Builder memBuilder = PlatformStatistics.Memory.newBuilder()
        .setHeap(buildPlatformMemoryUsage(mem.getHeapMemoryUsage()))
        .setNonHeap(buildPlatformMemoryUsage(mem.getNonHeapMemoryUsage()));

    for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
      MemoryUsage usage = pool.getUsage();
      if (usage == null) {
        continue;
      }
      PlatformStatistics.Memory.MemoryPool.Builder poolBuilder =
          PlatformStatistics.Memory.MemoryPool.newBuilder()
              .setName(pool.getName())
              .setUsage(buildPlatformMemoryUsage(usage));
      MemoryUsage collUsage = pool.getCollectionUsage();
      if (collUsage != null) {
        poolBuilder.setCollectionUsage(buildPlatformMemoryUsage(collUsage));
      }
      memBuilder.addPools(poolBuilder.build());
    }

    PlatformStatistics.Builder builder = PlatformStatistics.newBuilder()
        .setMemory(memBuilder.build())
        .setUptime(ManagementFactory.getRuntimeMXBean().getUptime());

    for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
      long count = gc.getCollectionCount();
      long time = gc.getCollectionTime();
      builder.putGc(gc.getName(), PlatformStatistics.Gc.newBuilder()
          .setTotal(count)
          .setAvgTime(count > 0 ? (double) time / count : 0.0)
          .build());
    }

    return builder.build();
  }

  private PlatformStatistics.Memory.MemoryUsage buildPlatformMemoryUsage(MemoryUsage u) {
    PlatformStatistics.Memory.MemoryUsage.Builder b = PlatformStatistics.Memory.MemoryUsage.newBuilder()
        .setCommitted(Math.max(0, u.getCommitted()));
    long used = Math.max(0, u.getUsed());
    if (used > 0) {
      b.setUsed(used);
    }
    if (u.getInit() > 0) {
      b.setInit(u.getInit());
    }
    if (u.getMax() > 0) {
      b.setMax(u.getMax());
    }
    return b.build();
  }

  private SystemStatistics buildSystemStatistics() {
    OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
    RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();

    double processCpu = 0.0, systemCpu = 0.0;
    try {
      Class<?> sunOs = Class.forName("com.sun.management.OperatingSystemMXBean");
      if (sunOs.isInstance(os)) {
        double p = (double) sunOs.getMethod("getProcessCpuLoad").invoke(os);
        if (p >= 0) {
          processCpu = p;
        }
        try {
          double s = (double) sunOs.getMethod("getCpuLoad").invoke(os);
          if (s >= 0) {
            systemCpu = s;
          }
        } catch (NoSuchMethodException ignored) {
          double s = (double) sunOs.getMethod("getSystemCpuLoad").invoke(os);
          if (s >= 0) {
            systemCpu = s;
          }
        }
      }
    } catch (Exception ignored) {
    }

    SystemStatistics.Cpu cpu = SystemStatistics.Cpu.newBuilder()
        .setThreads(os.getAvailableProcessors())
        .setProcessUsage(SystemStatistics.Cpu.Usage.newBuilder()
            .setLast15M(processCpu).setLast15M(processCpu).build())
        .setSystemUsage(SystemStatistics.Cpu.Usage.newBuilder()
            .setLast15M(systemCpu).setLast15M(systemCpu).build())
        .build();

    long totalPhysical = 0, usedPhysical = 0, totalSwap = 0, usedSwap = 0;
    try {
      Class<?> sunOs = Class.forName("com.sun.management.OperatingSystemMXBean");
      if (sunOs.isInstance(os)) {
        totalPhysical = (long) sunOs.getMethod("getTotalPhysicalMemorySize").invoke(os);
        long freePhysical = (long) sunOs.getMethod("getFreePhysicalMemorySize").invoke(os);
        totalSwap = (long) sunOs.getMethod("getTotalSwapSpaceSize").invoke(os);
        long freeSwap = (long) sunOs.getMethod("getFreeSwapSpaceSize").invoke(os);
        usedPhysical = Math.max(0, totalPhysical - freePhysical);
        usedSwap = Math.max(0, totalSwap - freeSwap);
      }
    } catch (Exception ignored) {
    }

    SystemStatistics.Memory memory = SystemStatistics.Memory.newBuilder()
        .setPhysical(SystemStatistics.Memory.MemoryPool.newBuilder()
            .setUsed(usedPhysical).setTotal(totalPhysical).build())
        .setSwap(SystemStatistics.Memory.MemoryPool.newBuilder()
            .setUsed(usedSwap).setTotal(totalSwap).build())
        .build();

    long totalDisk = 0, usedDisk = 0;
    try {
      java.io.File root = IS_WINDOWS
          ? new java.io.File(System.getenv().getOrDefault("SystemDrive", "C:") + "\\")
          : new java.io.File("/");
      totalDisk = root.getTotalSpace();
      usedDisk = totalDisk - root.getFreeSpace();
    } catch (Exception ignored) {
    }

    SystemStatistics.Builder builder = SystemStatistics.newBuilder()
        .setCpu(cpu)
        .setMemory(memory)
        .setDisk(SystemStatistics.Disk.newBuilder().setUsed(usedDisk).setTotal(totalDisk).build())
        .setOs(SystemStatistics.Os.newBuilder()
            .setArch(os.getArch()).setName(os.getName()).setVersion(os.getVersion()).build())
        .setJava(SystemStatistics.Java.newBuilder()
            .setVendor(System.getProperty("java.vendor", ""))
            .setVersion(System.getProperty("java.version", ""))
            .setVendorVersion(System.getProperty("java.vm.version", ""))
            .setVmArgs(String.join(" ", runtime.getInputArguments()))
            .build())
        .setJvm(SystemStatistics.Jvm.newBuilder()
            .setName(runtime.getVmName())
            .setVendor(runtime.getVmVendor())
            .setVersion(runtime.getVmVersion())
            .build())
        .setUptime(runtime.getUptime());

    for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
      long count = gc.getCollectionCount();
      long time = gc.getCollectionTime();
      builder.putGc(gc.getName(), SystemStatistics.Gc.newBuilder()
          .setTotal(count)
          .setAvgTime(count > 0 ? (double) time / count : 0.0)
          .setAvgFrequency(count > 0 ? (double) runtime.getUptime() / count : 0.0)
          .build());
    }

    return builder.build();
  }

  private ThreadNode buildThreadNode(String name, Node root) {
    List<Node> pool = new ArrayList<>();
    collectNodes(root, pool);

    ThreadNode.Builder builder = ThreadNode.newBuilder()
        .setName(name)
        .addTimes(root.time);

    for (Node child : root.children.values()) {
      builder.addChildrenRefs(child.index);
    }
    for (Node node : pool) {
      builder.addChildren(buildStackTraceNode(node));
    }
    return builder.build();
  }

  private StackTraceNode buildStackTraceNode(Node node) {
    StackTraceNode.Builder builder = StackTraceNode.newBuilder()
        .setClassName(node.className)
        .setMethodName(node.methodName)
        .addTimes(node.time);
    for (Node child : node.children.values()) {
      builder.addChildrenRefs(child.index);
    }
    return builder.build();
  }

  private void collectNodes(Node parent, List<Node> pool) {
    for (Node child : parent.children.values()) {
      child.index = pool.size();
      pool.add(child);
      collectNodes(child, pool);
    }
  }

  public enum Mode {CPU, ALLOC}

  static class Node {

    final String className;
    final String methodName;
    final Map<String, Node> children = new LinkedHashMap<>();
    double time;
    int index;

    Node(String cn, String mn) {
      className = cn;
      methodName = mn;
    }

    Node child(String cn, String mn) {
      return children.computeIfAbsent(cn + "#" + mn, k -> new Node(cn, mn));
    }
  }

  private record PluginSources(
      Map<String, String> classSources,
      Map<String, PluginOrModMetadata> pluginMeta
  ) {

  }
}