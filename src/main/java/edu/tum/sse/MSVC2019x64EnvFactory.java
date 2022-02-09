package edu.tum.sse;

import org.codehaus.mojo.natives.EnvFactory;
import org.codehaus.mojo.natives.NativeBuildException;
import org.codehaus.mojo.natives.msvc.EnvStreamConsumer;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.DefaultConsumer;
import org.codehaus.plexus.util.cli.StreamConsumer;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Map;

public class MSVC2019x64EnvFactory implements EnvFactory {
    public Map<String, String> getEnvironmentVariables() {
        String vcInstallDir = null;
        if (!System.getProperty("os.name").contains("Windows")) return null;
        try {
            Process p = Runtime.getRuntime().exec("\"C:\\Program Files (x86)\\Microsoft Visual Studio\\Installer\\vswhere.exe\" -format value -property installationPath");
            p.waitFor();
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = input.readLine()) != null) {
                if (line.contains("Microsoft Visual Studio")) {
                    vcInstallDir = line;
                    break;
                }
            }
            input.close();
        } catch (Exception e) {
            throw new NativeBuildException("Unable to construct Visual Studio install directory", e);
        }
        if (vcInstallDir == null) return null;
        Map<String, String> result;
        try {
            File tmpFile = File.createTempFile("msenv", ".bat");
            String buffer = "@echo off\r\n" +
                    "call \"" + vcInstallDir + "\"" + "\\VC\\Auxiliary\\Build\\vcvarsall.bat x64\n\r" +
                    "echo " + EnvStreamConsumer.START_PARSING_INDICATOR + "\r\n" +
                    "set\n\r";
            FileUtils.fileWrite(tmpFile.getAbsolutePath(), buffer);
            Commandline cl = new Commandline();
            cl.setExecutable(tmpFile.getAbsolutePath());
            StreamConsumer stderr = new DefaultConsumer();
            EnvStreamConsumer stdout = new EnvStreamConsumer();
            CommandLineUtils.executeCommandLine(cl, stdout, stderr);
            result = stdout.getParsedEnv();
        } catch (Exception e) {
            throw new NativeBuildException("Unable to execute Visual Studio vcvarsall.bat x64", e);
        }
        return result;
    }
}
