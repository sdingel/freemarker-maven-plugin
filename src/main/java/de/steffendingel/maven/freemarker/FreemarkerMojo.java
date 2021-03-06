//
// Copyright 2021 Steffen Dingel
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

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
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

/**
 * Apply a FreeMarker template to a set of JSON input files, writing one output file for each input file.
 */
// Note that the JavaDoc comments for this class and the fields annotated with @Parameter are used to generate Maven plugin help texts.
@Mojo(name = "freemarker", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class FreemarkerMojo extends AbstractMojo {

	/**
	 * Directory where the FreeMarker templates are located.
	 */
	@Parameter(defaultValue = "${project.basedir}/src/main/freemarker")
	private File templateDirectory;

	/**
	 * Name of the FreeMarker template file (including the extension).
	 */
	@Parameter(required = true)
	private String templateName;

	/**
	 * Set of JSON input files (models) for FreeMarker. The path of each file relative to the base directory of the file
	 * set is used to determine the output file path.
	 */
	@Parameter(required = true)
	private FileSet inputFiles;

	/**
	 * Directory to write the output files to. The output files are written in the same structure as the input files are
	 * found in the inputFiles file set.
	 */
	@Parameter(required = true)
	private File outputDirectory;

	/**
	 * File extension of the output file (without the dot).
	 */
	@Parameter(required = true)
	private String outputExtension;

	@Parameter(required = true, readonly = true, defaultValue = "${project.basedir}")
	private File projectBaseDirectory;

	@Override
	public void execute() throws MojoFailureException, MojoExecutionException {

		try {
			String projectBasePath = projectBaseDirectory.getAbsolutePath() + File.separator;

			Configuration cfg = new Configuration(Configuration.VERSION_2_3_30);
			cfg.setDirectoryForTemplateLoading(templateDirectory);
			cfg.setDefaultEncoding(StandardCharsets.UTF_8.name());
			cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
			cfg.setLogTemplateExceptions(false);
			cfg.setWrapUncheckedExceptions(true);
			cfg.setFallbackOnNullLoopVariable(false);

			Template template = cfg.getTemplate(templateName);
			getLog().info("Template: " + getLogPath(new File(templateDirectory, templateName), projectBasePath));

			Charset outputCharset = StandardCharsets.UTF_8;

			File inputDirectory = new File(inputFiles.getDirectory());
			for (String inputFileName : new FileSetManager().getIncludedFiles(inputFiles)) {

				// read JSON model
				File modelFile = new File(inputDirectory, inputFileName);
				getLog().info("Input:    " + getLogPath(modelFile, projectBasePath));
				Map<String, Object> dataModel = readJson(modelFile);

				// derive output file name and prepare output directory
				String modelFileBaseName;
				int extensionSeparatorPosition = inputFileName.lastIndexOf('.');
				if (extensionSeparatorPosition >= 0) {
					modelFileBaseName = inputFileName.substring(0, extensionSeparatorPosition);
				} else {
					modelFileBaseName = inputFileName;
				}
				File outputFile = new File(outputDirectory, modelFileBaseName + '.' + outputExtension);
				outputFile.getParentFile().mkdirs();

				// run FreeMarker and write output file
				getLog().info("Output:   " + getLogPath(outputFile, projectBasePath));
				try (OutputStream outputStream = new FileOutputStream(outputFile);
						Writer outputWriter = new OutputStreamWriter(outputStream, outputCharset)) {
					template.process(dataModel, outputWriter);
				}
			}
		} catch (TemplateException exc) {
			throw new MojoFailureException("FreeMarker template processing failed", exc);
		} catch (Exception exc) {
			throw new MojoExecutionException("Failed to run FreeMarker template engine", exc);
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

	private static String getLogPath(File file, String projectBasePath) {
		String logPath = file.getAbsolutePath();
		if (logPath.startsWith(projectBasePath)) {
			logPath = logPath.substring(projectBasePath.length());
		}
		return logPath;
	}

}