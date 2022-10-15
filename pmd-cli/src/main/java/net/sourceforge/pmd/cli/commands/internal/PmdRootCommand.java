package net.sourceforge.pmd.cli.commands.internal;

import net.sourceforge.pmd.PMDVersion;
import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;

// TODO : Status code 4 is actually contingent on using --fail-on-violation… we need to raise that to a common flag
@Command(name = "pmd", mixinStandardHelpOptions = true,
    versionProvider = PMDVersionProvider.class,
    exitCodeListHeading = "Exit Codes:%n",
    exitCodeList = { "0:Succesful analysis, no violations found", "1:An unexpected error occurred during execution",
            "2:Usage error, please refer to the command help", "4:Successful analysis, at least 1 violation found" },
    subcommands = { PmdCommand.class, CpdCommand.class })
public class PmdRootCommand {

}

class PMDVersionProvider implements IVersionProvider {

    @Override
    public String[] getVersion() throws Exception {
        return new String[] { "PMD " + PMDVersion.VERSION };
    }
}