package com.crowdin.cli.commands;

import com.crowdin.cli.commands.functionality.FilesInterface;
import com.crowdin.cli.properties.Params;
import com.crowdin.cli.properties.PropertiesBean;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public interface Actions {

    ClientAction download(
        FilesInterface files, boolean noProgress, String languageId, String branchName,
        boolean ignoreMatch, boolean isVerbose, Boolean skipTranslatedOnly,
        Boolean skipUntranslatedFiles, Boolean exportApprovedOnly, boolean plainView
    );

    Action generate(FilesInterface files, Path destinationPath, boolean skipGenerateDescription);

    ClientAction listBranches(boolean noProgress,  boolean plainView);

    ClientAction listProject(boolean noProgress, String branchName, boolean treeView, boolean plainView);

    ClientAction listSources(boolean noProgress, boolean treeView, boolean plainView);

    ClientAction listTranslations(boolean noProgress, boolean treeView, boolean isLocal, boolean plainView);

    ClientAction status(boolean noProgress, String languageId, boolean isVerbose, boolean showTranslated, boolean showApproved);

    ClientAction stringAdd(boolean noProgress, String text, String identifier, Integer maxLength, String context, List<String> files, Boolean hidden);

    ClientAction stringDelete(boolean noProgress, List<Long> ids, List<String> texts, List<String> identifiers);

    ClientAction stringEdit(
        boolean noProgress, Long id, String identifier, String newText, String newContext, Integer newMaxLength, Boolean isHidden);

    ClientAction stringList(boolean noProgress, boolean isVerbose, String file, String filter);

    ClientAction uploadSources(String branchName, boolean noProgress, boolean autoUpdate, boolean debug, boolean plainView);

    ClientAction uploadTranslations(
        boolean noProgress, String languageId, String branchName, boolean importEqSuggestions,
        boolean autoApproveImported, boolean debug, boolean plainView);

    Action checkNewVersion();

    Step<PropertiesBean> buildProperties(File configFile, File identityFile, Params params);

}
