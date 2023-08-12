package io.openems.edge.bridge.modbus.sunspec;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Objects;

import com.google.common.base.CaseFormat;
import com.google.gson.Gson;
import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingFunction;
import io.openems.edge.bridge.modbus.sunspec.SunSpecPoint.PointType;

/**
 * This tool converts SunSpec XML definitions to Java code suitable for the
 * OpenEMS SunSpec implementation.
 */
public class SunSpecCodeGenerator {

  /**
   * Path to the SunSpec model XML files; download them from
   * <a href="https://github.com/sunspec/models">SunSpec Models Github</a>.
   */
  private static final String SUNSPEC_GIT_URL = "https://github.com/sunspec/models/archive/refs/heads/master.zip";

  /**
   * Json files that should be ignored; mainly because certain features are not
   * implemented yet.
   */
  private static final Set<String> IGNORE_FILES = new HashSet<>(Arrays.asList(//
      "model_3.json", //
      "model_4.json", //
      "model_5.json", //
      "model_6.json", //
      "model_7.json", //
      "model_8.json", //
      "model_9.json", //
      "model_10.json", //
      "model_11.json", //
      "model_12.json", //
      "model_13.json", //
      "model_14.json", //
      "model_16.json", //
      "model_17.json", //
      "model_19.json", //
      "model_126.json", //
      "model_129.json", //
      "model_130.json", //
      "model_131.json", //
      "model_132.json", //
      "model_133.json", //
      "model_134.json", //
      "model_135.json", //
      "model_136.json", //
      "model_137.json", //
      "model_138.json", //
      "model_139.json", //
      "model_140.json", //
      "model_141.json", //
      "model_142.json", //
      "model_143.json", //
      "model_144.json", //
      "model_160.json", //
      "model_211.json", //
      "model_212.json", //
      "model_213.json", //
      "model_214.json", //
      "model_220.json", //
      "model_401.json", //
      "model_402.json", //
      "model_403.json", //
      "model_404.json", //
      "model_501.json", //
      "model_502.json", //
      "model_601.json", //
      "model_803.json", //
      "model_804.json", //
      "model_805.json", //
      "model_806.json", //
      "model_807.json", //
      "model_808.json", //
      "model_809.json", //
      "model_63001.json", //
      "model_63002.json", //
      "model_64020.json" //
  ));

  /**
   * Run this method to start the code generator.
   *
   * @param args not supported
   * @throws Exception on error
   */
  public static void main(String[] args) throws Exception {
    Path modelsDir = downloadSunSpecFiles();
    var models = parseSunSpecFiles(modelsDir);
    writeSunSpecModelJavaFile(models);
  }

  private static Path downloadSunSpecFiles() throws IOException {
    Path tempDir = Files.createTempDirectory("zipextract");
    URL url = new URL(SUNSPEC_GIT_URL);
    try (InputStream in = url.openStream()) {
      Files.copy(in, tempDir.resolve("temp.zip"));
    }
    Path zipFilePath = tempDir.resolve("temp.zip");
    unzip(zipFilePath.toString(), tempDir.toString());
    return tempDir.resolve("models-master/json");
  }

  private static void unzip(String zipFilePath, String destDir) throws IOException {
    try (ZipFile zipFile = new ZipFile(zipFilePath)) {
      Enumeration<? extends ZipEntry> entries = zipFile.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        String entryName = entry.getName();
        Path entryPath = Paths.get(destDir, entryName);
        if (entry.isDirectory()) {
          Files.createDirectories(entryPath);
        } else {
          try (InputStream is = zipFile.getInputStream(entry);
               OutputStream os = Files.newOutputStream(entryPath)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
              os.write(buffer, 0, length);
            }
          }
        }
      }
    }
  }

  /**
   * Parses all SunSpec XML files in a directory.
   *
   * @param folder the folder containing the json files
   * @return a list of Models
   * @throws Exception on error
   */
  private static List<Model> parseSunSpecFiles(Path folder) throws Exception {
    Gson gson = new Gson();
    List<Model> result = new ArrayList<>();
    for (File file : Objects.requireNonNull(folder.toFile().listFiles(file -> //
        file.getName().startsWith("model") //
            && file.getName().endsWith(".json") //
            && !IGNORE_FILES.contains(file.getName())))) {
      try {
        result.add(gson.fromJson(Files.newBufferedReader(file.toPath()), Model.class));
      } catch (Exception e) {
        throw new Exception("Error while reading from " + file, e);
      }
    }
    result.sort(Comparator.comparing(Model::getId));
    return result;
  }

  /**
   * Writes the SunSpecModel.java file.
   *
   * @param models a list of Models
   * @throws IOException on error
   */
  private static void writeSunSpecModelJavaFile(List<Model> models) throws IOException, URISyntaxException, OpenemsNamedException {
    File location = new File(SunSpecCodeGenerator.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
    String filePath =
        location.getParent() + File.separator + "src" + File.separator + "io.openems.edge.bridge.modbus".replace(".", File.separator)
            + File.separator + "sunspec" + File.separator + "DefaultSunSpecModel.java";
    try (var w = Files.newBufferedWriter(Paths.get(filePath))) {
      w.write("""
            // CHECKSTYLE:OFF
            
          package io.openems.edge.bridge.modbus.sunspec;
                 
          import io.openems.common.channel.AccessMode;
          import io.openems.common.channel.Unit;
          import io.openems.common.types.OptionsEnum;
                 
          /**
           * Do not touch this file. It is auto-generated by SunSpecCodeGenerator.
           */
          public enum DefaultSunSpecModel implements SunSpecModel {
          		""");

      /*
       * Write main Model enum
       */
      for (var i = 0; i < models.size(); i++) {
        var model = models.get(i);
        w.write("	S_" + model.id + "(//");
        w.newLine();
        w.write("			\"" + esc(model.group.label) + "\", //");
        w.newLine();
        w.write("			\"" + esc(model.group.desc) + "\", //");
        w.newLine();
        w.write("			\"" + esc(model.group.notes) + "\", //");
        w.newLine();
        w.write("			" + model.group.getLength() + ", //");
        w.newLine();
        w.write("			S" + model.id + ".values(), //");
        w.newLine();
        w.write("			SunSpecModelType." + model.getType() + " //");
        w.newLine();
        w.write("	)");
        if (i == models.size() - 1) {
          w.write("; //");
        } else {
          w.write(", //");
        }
        w.newLine();
      }
      w.newLine();

      /*
       * For each Model write enum with SunSpecPoints
       */
      for (Model model : models) {
        w.write("	public static enum S" + model.id + " implements SunSpecPoint {");
        w.newLine();
        for (var i = 0; i < model.group.points.size(); i++) {
          var point = model.group.points.get(i);
          var pointUpperId = toUpperUnderscore(point.name);
          w.write("		" + pointUpperId + "(new PointImpl(//");
          w.newLine();
          w.write("				\"S" + model.id + "_" + pointUpperId + "\", //");
          w.newLine();
          w.write("				\"" + esc(point.label) + "\", //");
          w.newLine();
          w.write("				\"" + esc(point.desc) + "\", //");
          w.newLine();
          w.write("				\"" + esc(point.notes) + "\", //");
          w.newLine();
          w.write("				PointType." + point.getType().name() + ", //");
          w.newLine();
          w.write("				" + point.isMandatory() + ", //");
          w.newLine();
          w.write("				AccessMode." + point.getAccess().name() + ", //");
          w.newLine();
          w.write("				Unit." + point.getUnit().name() + ", //");
          w.newLine();
          w.write("				" + Optional.ofNullable(point.sf).map(s -> "\"" + s + "\"").orElse(null) + ", //");
          w.newLine();
          if (point.getSymbols().isEmpty()) {
            w.write("				new OptionsEnum[0]))");
          } else {
            w.write("				S" + model.id + "_" + point.name + ".values()))");
          }

          if (i == model.group.points.size() - 1) {
            w.write("; //");
          } else {
            w.write(", //");
          }
          w.newLine();
        }
        w.newLine();
        w.write("		protected final PointImpl impl;");
        w.newLine();
        w.newLine();
        w.write("		private S" + model.id + "(PointImpl impl) {");
        w.newLine();
        w.write("			this.impl = impl;");
        w.newLine();
        w.write("		}");
        w.newLine();
        w.newLine();
        w.write("		@Override");
        w.newLine();
        w.write("		public PointImpl get() {");
        w.newLine();
        w.write("			return this.impl;");
        w.newLine();
        w.write("		}");
        w.newLine();
        w.write("	}");
        w.newLine();
        w.newLine();

        /*
         * For SunSpecPoints with Symbols write OpenEMS OptionsEnum
         */
        for (Point point : model.group.points) {
          if (point.getSymbols().isEmpty()) {
            continue;
          }

          w.write("	public static enum S" + model.id + "_" + point.name + " implements OptionsEnum {");
          w.newLine();
          w.write("		UNDEFINED(-1, \"Undefined\"), //");
          w.newLine();
          for (var i = 0; i < point.symbols.size(); i++) {
            var symbol = point.symbols.get(i);
            var symbolId = symbol.getId();
            symbolId = toUpperUnderscore(symbolId);

            if (symbolId.equals("RESERVED")) {
              symbolId = symbolId + "_" + symbol.value; // avoid duplicated "RESERVED" ids.
            }

            w.write("		" + symbolId + "(" + symbol.value + ", \"" + symbolId + "\")");
            if (i == point.symbols.size() - 1) {
              w.write("; //");
            } else {
              w.write(", //");
            }
            w.newLine();
          }
          w.write("""

              		private final int value;
              		private final String name;
              	   
              		private %s(int value, String name) {
              			this.value = value;
              			this.name = name;
              		}
              	   
              		@Override
              		public int getValue() {
              			return this.value;
              		}
              	   
              		@Override
              		public String getName() {
              			return this.name;
              		}
              	   
              		@Override
              		public OptionsEnum getUndefined() {
              			return UNDEFINED;
              		}
              	}

              """.formatted("S" + model.id + "_" + point.name));
        }
      }
      w.write("""
          	public final String label;
          	public final String description;
          	public final String notes;
          	public final int length;
          	public final SunSpecPoint[] points;
          	public final SunSpecModelType modelType;
               
          	private DefaultSunSpecModel(String label, String description, String notes, int length, SunSpecPoint[] points,
          			SunSpecModelType modelType) {
          		this.label = label;
          		this.description = description;
          		this.notes = notes;
          		this.length = length;
          		this.points = points;
          		this.modelType = modelType;
          	}
               
          	@Override
          	public SunSpecPoint[] points() {
          		return this.points;
          	}
               
          	@Override
          	public String label() {
          		return this.label;
          	}
          }
          // CHECKSTYLE:ON
          """);
    }
  }

  /**
   * Helper method to escape a string.
   *
   * @param string original string
   * @return escaped string
   */
  private static String esc(String string) {
    if (string == null) {
      return "";
    }
    return string //
        .replaceAll("[^\\x00-\\x7F]", "") // non-ascii chars
        .replace("\"", "\\\"") // escape backslash
        .trim();
  }

  public static class Model {
    private final int id;
    private final ModelGroup group;

    public Model(int id, ModelGroup group) {
      this.id = id;
      this.group = group;
    }

    public int getId() {
      return this.id;
    }

    public SunSpecModelType getType() {
      return SunSpecModelType.getModelType(this.id);
    }

  }

  public static class ModelGroup {
    private final String name;
    private final String label;
    private final String desc;
    private final String notes;
    private final List<Point> points;

    public ModelGroup(String name, String label, String desc, String notes, List<Point> points) {
      this.name = name;
      this.label = label;
      this.desc = desc;
      this.notes = notes;
      this.points = points;
    }

    public int getLength() {
      return this.points.stream().map(p -> p.size).reduce(0, Integer::sum);
    }

    public String getName() {
      return this.name;
    }

    public List<Point> getPoints() {
      return this.points;
    }
  }

  public static class Point {
    private final String name;
    private final int size;
    private final String label;
    private final String desc;
    private final String notes;
    private final String sf;
    private final String type;
    private final String access;
    private final String mandatory;
    private final String units;
    private final List<Symbol> symbols;

    public Point(String name, int size, String label, String desc, String notes, String sf, String type, String access, String mandatory,
                 String units, List<Symbol> symbols) {
      this.name = name;
      this.size = size;
      this.label = label;
      this.desc = desc;
      this.notes = notes;
      this.sf = sf;
      this.type = type;
      this.access = access;
      this.mandatory = mandatory;
      this.units = units;
      this.symbols = symbols;
    }


    /**
     * Helper method return PointType.
     *
     * @return PointType
     */
    public PointType getType() {
      return this.type.equals("string") ? PointType.valueOf("STRING" + this.size) : PointType.valueOf(this.type.toUpperCase());
    }

    /**
     * Helper method return Unit.
     *
     * @return Unit
     */
    public Unit getUnit() throws OpenemsNamedException {
      return toUnit(Optional.ofNullable(this.units).orElse(""));
    }


    /**
     * Helper method return AccessMode.
     *
     * @return AccessMode
     */
    public AccessMode getAccess() {
      return switch (Optional.ofNullable(this.access).orElse("").toLowerCase()) {
        case "wo" -> AccessMode.WRITE_ONLY;
        case "rw" -> AccessMode.READ_WRITE;
        default -> AccessMode.READ_ONLY;
      };
    }

    public boolean isMandatory() {
      return "M".equals(this.mandatory);
    }

    static Unit toUnit(String unit) throws OpenemsNamedException {
      final ThrowingFunction<String, Unit, OpenemsNamedException> toUnit = s -> {
        s = s.trim();
        if (s.contains(" ")) {
          s = s.substring(0, s.indexOf(" "));
        }
        switch (s) {
          // not available in OpenEMS
          case "", "%ARtg/%dV", "bps", "cos()", "deg", "Degrees", "hhmmss", "hhmmss.sssZ", "HPa", "kO", "Mbps", "meters", "mm", "mps", "m/s", "ohms", "Pct", "PF", "SF", "text", "Tmd", "Tmh", "Tms", "Various", "Vm", "W/m2", "YYYYMMDD", "S", "%Max/Sec" -> { // not available in OpenEMS
            return Unit.NONE;
          }
          case "%", "%WHRtg" -> {
            return Unit.PERCENT;
          }
          case "A" -> {
            return Unit.AMPERE;
          }
          case "Ah", "AH" -> {
            return Unit.AMPERE_HOURS;
          }
          case "C" -> {
            return Unit.DEGREE_CELSIUS;
          }
          case "Hz" -> {
            return Unit.HERTZ;
          }
          case "kAH" -> {
            return Unit.KILOAMPERE_HOURS;
          }
          case "kWh" -> {
            return Unit.KILOWATT_HOURS;
          }
          case "mSecs" -> {
            return Unit.MILLISECONDS;
          }
          case "Secs" -> {
            return Unit.SECONDS;
          }
          case "V" -> {
            return Unit.VOLT;
          }
          case "VA" -> {
            return Unit.VOLT_AMPERE;
          }
          case "VAh" -> {
            return Unit.VOLT_AMPERE_HOURS;
          }
          case "var", "Var" -> {
            return Unit.VOLT_AMPERE_REACTIVE;
          }
          case "varh", "Varh" -> {
            return Unit.VOLT_AMPERE_REACTIVE_HOURS;
          }
          case "W" -> {
            return Unit.WATT;
          }
          case "Wh", "WH" -> {
            // Validate manually: OpenEMS distinguishes CUMULATED and DISCRETE Watt-Hours.
            return Unit.CUMULATED_WATT_HOURS;
          }
        }
        throw new OpenemsException("Unhandled unit [" + s + "]");
      };
      return toUnit.apply(unit);
    }

    public String getName() {
      return this.name;
    }

    public int getSize() {
      return this.size;
    }

    public List<Symbol> getSymbols() {
      return Optional.ofNullable(this.symbols).orElse(List.of());
    }
  }

  public static class Symbol {
    private final String name;
    private final int value;

    public Symbol(String name, int value) {
      this.name = name;
      this.value = value;
    }

    public String getId() {
      return switch (this.name) {
        case "ggOFF", "ggSLEEPING", "ggSTARTING", "ggTHROTTLED", "ggSHUTTING_DOWN", "ggFAULT", "ggSTANDBY" ->
            // Special handling for ID 111 point "Operating State"
            // TODO: create pull-request to fix XML file upstream
            this.name.substring(2);
        case "M_EVENT_Power_Failure", "M_EVENT_Under_Voltage", "M_EVENT_Low_PF", "M_EVENT_Over_Current", "M_EVENT_Over_Voltage", "M_EVENT_Missing_Sensor", "M_EVENT_Reserved1", "M_EVENT_Reserved2", "M_EVENT_Reserved3", "M_EVENT_Reserved4", "M_EVENT_Reserved5", "M_EVENT_Reserved6", "M_EVENT_Reserved7", "M_EVENT_Reserved8", "M_EVENT_OEM01", "M_EVENT_OEM02", "M_EVENT_OEM03", "M_EVENT_OEM04", "M_EVENT_OEM05", "M_EVENT_OEM06", "M_EVENT_OEM07", "M_EVENT_OEM08", "M_EVENT_OEM09", "M_EVENT_OEM10", "M_EVENT_OEM11", "M_EVENT_OEM12", "M_EVENT_OEM13", "M_EVENT_OEM14", "M_EVENT_OEM15" ->
            // Special handling for ID 202 point "Events"
            this.name.substring(8);
        default -> this.name;
      };
    }

    public int getValue() {
      return this.value;
    }
  }

  protected static String toUpperUnderscore(String string) {
    string = string //
        .replace("-", "_") //
        .replace(" ", "_");
    if (!string.toUpperCase().equals(string)) {
      string = CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, string);
    }
    return string.replace("__", "_");
  }

}
