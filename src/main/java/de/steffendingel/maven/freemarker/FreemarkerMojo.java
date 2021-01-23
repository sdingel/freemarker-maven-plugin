package de.steffendingel.maven.freemarker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;

@Mojo(name = "freemarker")
public class FreemarkerMojo extends AbstractMojo {

	@Parameter(name = "freemarker-version", property = "freemarker.version", defaultValue = "2.3.30")
	private String freemarkerVersion;

	@Parameter(name = "template-directory", property = "freemarker.template.directory", defaultValue = "src/main/freemarker")
	private File templateDirectory;

	@Parameter(required = true)
	private String templateName;
	
	@Parameter(name = "template-default-encoding", property = "freemarker.template.default.encoding", defaultValue = "UTF-8")
	private String templateDefaultEncoding;

	@Parameter(required = true)
	private FileSet modelFiles;

	@Parameter(required = true)
	private File outputDirectory;
	
	@Parameter(required = true)
	private String outputExtension;
	
	public void execute() throws MojoExecutionException {
		
		getLog().info("Running freemarker ...");

		try {
			
			Configuration cfg = new Configuration(Configuration.VERSION_2_3_30); // TODO

			cfg.setDirectoryForTemplateLoading(templateDirectory);

			cfg.setDefaultEncoding(templateDefaultEncoding);

			// Sets how errors will appear.
			// During web page *development* TemplateExceptionHandler.HTML_DEBUG_HANDLER is
			// better.
			cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);

			// Don't log exceptions inside FreeMarker that it will thrown at you anyway:
			cfg.setLogTemplateExceptions(false);

			// Wrap unchecked exceptions thrown during template processing into
			// TemplateException-s:
			cfg.setWrapUncheckedExceptions(true);

			// Do not fall back to higher scopes when reading a null loop variable:
			cfg.setFallbackOnNullLoopVariable(false);

			getLog().info("Template name: " + templateName);
			Template template = cfg.getTemplate(templateName);

			Charset outputCharset = StandardCharsets.UTF_8;

			File modelDirectory = new File(modelFiles.getDirectory());
			for (String modelFileName : new FileSetManager().getIncludedFiles(modelFiles)) {
				File modelFile = new File(modelDirectory, modelFileName);
				getLog().info("Reading " + modelFile.getPath());
				Map<String, Object> dataModel = readJson(modelFile);
				String modelFileBaseName;
				int extensionSeparatorPosition = modelFileName.lastIndexOf('.');
				if (extensionSeparatorPosition >= 0) {
					modelFileBaseName =  modelFileName.substring(0, extensionSeparatorPosition);
				} else {
					modelFileBaseName = modelFileName;
				}
				File outputFile = new File(outputDirectory, modelFileBaseName + '.' + outputExtension);
				outputFile.getParentFile().mkdirs();
				getLog().info("Writing " + outputFile.getPath());
				try (OutputStream outputStream = new FileOutputStream(outputFile);
						Writer outputWriter = new OutputStreamWriter(outputStream, outputCharset)) {
					template.process(dataModel, outputWriter);
				}
			}
		} catch (Exception exc) {
			throw new MojoExecutionException("Freemarker template processing failed", exc);
		}
	}

	private static Map<String, Object> readJson(File file) throws IOException {
		try (InputStream input = new FileInputStream(file); JsonReader reader = Json.createReader(input)) {
			return jsonObjectToMap(reader.readObject());
		}
	}

	private static Object jsonValueToObject(JsonValue jsonValue) {
		switch (jsonValue.getValueType()) {
		case ARRAY:
			return jsonArrayToList(jsonValue.asJsonArray());
		case OBJECT:
			return jsonObjectToMap(jsonValue.asJsonObject());
		case STRING:
			return ((JsonString) jsonValue).getChars();
		case NUMBER:
			return ((JsonNumber) jsonValue).bigDecimalValue();
		case TRUE:
			return Boolean.TRUE;
		case FALSE:
			return Boolean.FALSE;
		case NULL:
			return null;
		default:
			throw new IllegalArgumentException("Unknown JSON value type " + jsonValue.getValueType().name());
		}
	}

	private static List<Object> jsonArrayToList(JsonArray jsonArray) {
		return jsonArray.stream().map(FreemarkerMojo::jsonValueToObject).collect(Collectors.toList());
	}

	private static Map<String, Object> jsonObjectToMap(JsonObject jsonObject) {
		Map<String, Object> map = new LinkedHashMap<>();
		jsonObject.forEach((name, value) -> {
			map.put(name, jsonValueToObject(value));
		});
		return map;
	}

}