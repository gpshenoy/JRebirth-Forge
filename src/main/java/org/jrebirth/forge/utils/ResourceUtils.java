package org.jrebirth.forge.utils;

import static org.jrebirth.forge.utils.PluginUtils.messages;

import java.io.FileNotFoundException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.forge.parser.JavaParser;
import org.jboss.forge.parser.java.JavaInterface;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.project.facets.MetadataFacet;
import org.jboss.forge.resources.DirectoryResource;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.ShellMessages;
import org.jboss.forge.shell.plugins.PipeOut;
import org.jboss.forge.shell.util.Packages;
import org.jrebirth.forge.utils.PluginUtils.CreationType;

/**
 * The Class ResourceUtils.
 */
public class ResourceUtils {

    /** The Constant COLOR_RGB01_PATTERN. */
    private static final String COLOR_RGB01_PATTERN = "((0\\.[0-9]*|1\\.0)\\s*?_\\s*?){2}(0\\.[0-9]*|1\\.0)"; // 0.0-1.0,0.0-1.0,0.0-1.0

    /** The Constant COLOR_HSB_PATTERN. */
    private static final String COLOR_HSB_PATTERN = "((0\\.[0-9]*|([012]?[0-9]?[0-9]|3[0-5][0-9])\\.[0-9]*|360.0)\\s*?_\\s*?){2}(0\\.[0-9]*|([012]?[0-9]?[0-9]|3[0-5][0-9])\\.[0-9]*|360.0)"; // 0.0-360.0,0.0-1.0,0.0-1.0

    /** The Constant COLOR_RGB255_PATTERN. */
    private static final String COLOR_RGB255_PATTERN = "(([01]?[0-9]?[0-9]|2[0-4][0-9]|25[0-5])\\s*?_\\s*?){2}([01]?[0-9]?[0-9]|2[0-4][0-9]|25[0-5])"; // 0-255,0-255,0-255

    /** The Constant COLOR_GRAY_PATTERN. */
    private static final String COLOR_GRAY_PATTERN = "(0\\.[0-9]*|1\\.0)"; // [0.0-1.0]

    /** The Constant COLOR_WEB_PATTERN. */
    private static final String COLOR_WEB_PATTERN = "[0-9A-F]{6}"; // [0-9A-F]{6}

    /**
     * Validate color value using type.
     * 
     * @param colorType the color type
     * @param colorValue the color value
     * @return true, if successful
     */
    public static boolean validateColorValueUsingType(final String colorType, final String colorValue) {

        Pattern pattern = null;

        if (colorType.equalsIgnoreCase("web")) {

            pattern = Pattern.compile(COLOR_WEB_PATTERN);

        } else if (colorType.equalsIgnoreCase("gray")) {

            pattern = Pattern.compile(COLOR_GRAY_PATTERN);

        } else if (colorType.equalsIgnoreCase("hsb")) {

            pattern = Pattern.compile(COLOR_HSB_PATTERN);

        } else if (colorType.equalsIgnoreCase("rgb01")) {

            pattern = Pattern.compile(COLOR_RGB01_PATTERN);

        } else if (colorType.equalsIgnoreCase("rgb255")) {

            pattern = Pattern.compile(COLOR_RGB255_PATTERN);
        }

        if (pattern != null) {

            Matcher matcher = pattern.matcher(colorValue);
            return matcher.matches();

        } else {
            return false;
        }

    }

    /**
     * Add a color resource.
     * 
     * @param project the project
     * @param shell the shell
     * @param out the out
     * @param colorName the name of the color variable
     * @param colorValue the color value
     * @param colorType the color type
     * @param opacityValue the opacity value
     */
    public static void manageColorResource(final Project project, final Shell shell, final PipeOut out, final String colorName, final String colorValue, final String colorType,
            final double opacityValue) {
        DirectoryResource directory = null;
        final MetadataFacet metadata = project.getFacet(MetadataFacet.class);
        final DirectoryResource sourceFolder = project.getFacet(JavaSourceFacet.class).getSourceFolder();
        final String topLevelPackage = metadata.getTopLevelPackage();

        String type = ((colorType.equalsIgnoreCase("web") ? "Web" : (colorType.equalsIgnoreCase("gray") ? "Gray" : colorType.toUpperCase())));

        final String importString = "org.jrebirth.core.resource.color." + type + "Color";

        final String fieldDefinition = " /** Color constant for {}. */\n   ColorItem {} = " + buildConstructorByColorType(type, colorValue, opacityValue) + "\n\n";

        directory = sourceFolder.getChildDirectory(Packages.toFileSyntax(topLevelPackage + CreationType.RESOURCE.getPackageName() + "."));
        if (directory.isDirectory() == false || directory.getChild(metadata.getProjectName() + "Colors.java").exists() == false) {
            try {
                ShellMessages.info(out, messages.getMessage("color.is.not.created"));
                shell.execute("jrebirth resource-create --all false --colorGenerate");
            } catch (final Exception e) {
                ShellMessages.error(out, messages.getMessage("unable.to.create.color"));
            }
        }
        final JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);
        final JavaInterface jInterface = JavaParser.parse(JavaInterface.class, directory.getChild(metadata.getProjectName() + "Colors.java").getResourceInputStream());

        final String capsColorName = StringUtils.camelCaseToUnderscore(colorName);

        if (jInterface.hasImport(importString) == false)
            jInterface.addImport(importString);

        if (jInterface.hasField(capsColorName) == false) {

            jInterface.addField(fieldDefinition.replaceAll("\\{\\}", capsColorName));

            try {

                java.saveJavaSource(jInterface);
            } catch (final FileNotFoundException e) {
                ShellMessages.error(out, messages.getMessage("unable.to.save.file", capsColorName));
            }
        }
        else {
            ShellMessages.warn(out, messages.getMessage("color.constant.already.exist", capsColorName));
        }
    }

    private static String buildConstructorByColorType(final String type, final String colorValue, final double opacityValue) {

        String constructor = "";

        if (type.equalsIgnoreCase("web")) {

            constructor = "create(new WebColor(\"" + colorValue.toUpperCase() + "\", " + opacityValue + "));";

        } else if (type.equalsIgnoreCase("gray")) {

            constructor = "create(new GrayColor(" + colorValue + ", " + opacityValue + "));";

        } else if (type.equalsIgnoreCase("hsb")) {

            String[] hsb = colorValue.split("_");
            constructor = "create(new HSBColor(" + hsb[0] + ", " + hsb[1] + ", " + hsb[2] + ", " + opacityValue + "));";

        } else if (type.equalsIgnoreCase("rgb01")) {

            String[] rgb = colorValue.split("_");
            constructor = "create(new RGB01Color(" + rgb[0] + ", " + rgb[1] + ", " + rgb[2] + ", " + opacityValue + "));";

        } else if (type.equalsIgnoreCase("rgb255")) {

            String[] rgb = colorValue.split("_");
            constructor = "create(new RGB255Color(" + rgb[0] + ", " + rgb[1] + ", " + rgb[2] + ", " + opacityValue + "));";

        }

        return constructor;

    }

}
