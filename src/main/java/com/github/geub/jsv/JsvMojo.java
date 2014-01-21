package com.github.geub.jsv;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(name = "jsv")
public class JsvMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	private MavenProject project;

	@Parameter(defaultValue = "${basedir}", required = true, readonly = true)
	private String basedir;

	private static Pattern jsFilenamePattern = Pattern.compile("(.+?)(-([\\d\\.\\-]+))?\\.js");

	public void execute() throws MojoExecutionException {
		String version = this.project.getVersion();
		final Map<String, String> replacedNames = new HashMap<String, String>();

		File directory = new File(this.basedir);
		Collection<File> jsFiles = FileUtils.listFiles(directory, this.jsFileFilter, TrueFileFilter.INSTANCE);
		for (File file : jsFiles) {
			try {
				copyJavascriptFilesUpdatingVersion(replacedNames, JsvMojo.jsFilenamePattern, file, version);
			} catch (IOException e) {
				throw new MojoExecutionException("Fail processing javascript files", e);
			}
		}
		Collection<File> filesToProcess = FileUtils.listFiles(directory, this.filesToProcessFileFilter, TrueFileFilter.INSTANCE);
		for (File file : filesToProcess) {
			try {
				replaceJsNameInSourceFiles(replacedNames, file);
			} catch (IOException e) {
				throw new MojoExecutionException("Fail replacing javascript import in files", e);
			}
		}

	}

	private void copyJavascriptFilesUpdatingVersion(final Map<String, String> replacedNames, Pattern pattern, File file, String version) throws IOException {
		String filename = file.getName();
		Matcher matcher = pattern.matcher(filename);
		if (matcher.matches()) {
			String name = matcher.group(1);
			String newName = name + "-" + version + ".js";
			replacedNames.put(filename, newName);
			FileUtils.moveFile(file, new File(file.getParent() + "/" + newName));
		}
	}

	private void replaceJsNameInSourceFiles(final Map<String, String> replacedNames, File file) throws IOException {
		if (!file.isFile()) {
			return;
		}
		boolean lineChanged = false;
		StringBuilder newFileContent = new StringBuilder();
		for (String line : FileUtils.readLines(file, "UTF-8")) {
			for (String nameInMap : replacedNames.keySet()) {
				if (line.contains(nameInMap)) {
					newFileContent.append(line.replace(nameInMap, replacedNames.get(nameInMap)));
					lineChanged = true;
				} else {
					newFileContent.append(line);
				}
				newFileContent.append(System.getProperty("line.separator"));
			}
		}
		if (!lineChanged) {
			return;
		}
		FileUtils.deleteQuietly(file);
		FileUtils.touch(file);
		FileUtils.writeStringToFile(file, newFileContent.toString(), "UTF-8");
	}

	private IOFileFilter jsFileFilter = new IOFileFilter() {
		public boolean accept(File dir, String name) {
			return false;
		}

		public boolean accept(File file) {
			return file.getName().endsWith(".js");
		}
	};

	private IOFileFilter filesToProcessFileFilter = new IOFileFilter() {
		public boolean accept(File dir, String name) {
			return false;
		}

		public boolean accept(File file) {
			return file.getName().endsWith(".jsp") || file.getName().endsWith(".tag") || file.getName().endsWith(".html");
		}
	};

}
