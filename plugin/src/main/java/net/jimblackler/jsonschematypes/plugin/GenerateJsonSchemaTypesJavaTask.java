package net.jimblackler.jsonschematypes.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import net.jimblackler.codegen.CodeGenerator;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskAction;

public class GenerateJsonSchemaTypesJavaTask extends DefaultTask {
  @TaskAction
  public void generate() throws IOException {
    Project project = getProject();
    SourceSet mainSourceSet = ((SourceSetContainer) project.getProperties().get("sourceSets"))
                                  .getByName(SourceSet.MAIN_SOURCE_SET_NAME);
    JsonSchemaTypesPluginExtension extension =
        project.getExtensions().getByType(JsonSchemaTypesPluginExtension.class);
    File resourcesDir = mainSourceSet.getOutput().getResourcesDir();
    Path resources = resourcesDir.toPath().resolve(extension.getResourcesPath());
    Path codePath = Common.getCodePath(getProject());
    CodeGenerator.outputTypes(codePath, extension.getPackageOut(), resources.toUri().toURL());
  }
}
