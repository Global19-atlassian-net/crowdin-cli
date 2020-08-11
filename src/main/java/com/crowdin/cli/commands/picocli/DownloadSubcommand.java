package com.crowdin.cli.commands.picocli;

import com.crowdin.cli.commands.Actions;
import com.crowdin.cli.commands.ClientAction;
import com.crowdin.cli.commands.functionality.FsFiles;
import picocli.CommandLine;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@CommandLine.Command(
    name = CommandNames.DOWNLOAD,
    sortOptions = false,
    aliases = CommandNames.ALIAS_DOWNLOAD)
class DownloadSubcommand extends ClientActPlainMixin {

    @CommandLine.Option(names = {"-b", "--branch"}, paramLabel = "...")
    protected String branchName;

    @CommandLine.Option(names = {"--ignore-match"})
    protected boolean ignoreMatch;

    @CommandLine.Option(names = {"-l", "--language"}, paramLabel = "...")
    protected String languageId;

    @CommandLine.Option(names = {"--dryrun"})
    protected boolean dryrun;

    @CommandLine.Option(names = {"--tree"}, descriptionKey = "tree.dryrun")
    protected boolean treeView;

    @CommandLine.Option(names = {"--skip-untranslated-strings"}, descriptionKey = "crowdin.download.skipUntranslatedStrings")
    protected Boolean skipTranslatedOnly;

    @CommandLine.Option(names = {"--skip-untranslated-files"}, descriptionKey = "crowdin.download.skipUntranslatedFiles")
    protected Boolean skipUntranslatedFiles;

    @CommandLine.Option(names = {"--export-only-approved"}, descriptionKey = "crowdin.download.exportOnlyApproved")
    protected Boolean exportApprovedOnly;

    @Override
    protected ClientAction getAction(Actions actions) {
        return (dryrun)
            ? actions.listTranslations(noProgress, treeView, false, plainView)
            : actions.download(
                new FsFiles(), noProgress, languageId, branchName, ignoreMatch, isVerbose,
                skipTranslatedOnly, skipUntranslatedFiles, exportApprovedOnly, plainView);
    }

    @Override
    protected List<String> checkOptions() {
        if (skipTranslatedOnly != null && skipUntranslatedFiles != null && skipTranslatedOnly && skipUntranslatedFiles) {
            return Arrays.asList(RESOURCE_BUNDLE.getString("error.skip_untranslated_both_strings_and_files"));
        }
        return Collections.emptyList();
    }
}
