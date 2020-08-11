package com.crowdin.cli.commands.functionality;

import com.crowdin.cli.client.LanguageMapping;
import com.crowdin.cli.properties.PropertiesBean;
import com.crowdin.cli.utils.PlaceholderUtil;
import com.crowdin.cli.utils.Utils;
import com.crowdin.client.languages.model.Language;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DryrunTranslations extends Dryrun {

    private PropertiesBean pb;
    private PlaceholderUtil placeholderUtil;
    private boolean filesMustExist;
    private LanguageMapping projectLanguageMapping;
    private Optional<Language> language;

    public DryrunTranslations(
        PropertiesBean pb, LanguageMapping projectLanguageMapping,
        PlaceholderUtil placeholderUtil, Optional<Language> language, boolean filesMustExist
    ) {
        super("message.translation_file");
        this.pb = pb;
        this.placeholderUtil = placeholderUtil;
        this.filesMustExist = filesMustExist;
        this.projectLanguageMapping = projectLanguageMapping;
        this.language = language;
    }

    @Override
    protected List<String> getFiles() {
        return pb.getFiles()
            .stream()
            .flatMap(file -> SourcesUtils.getFiles(pb.getBasePath(), file.getSource(), file.getIgnore(), placeholderUtil)
                .map(source -> {
                    String fileSource = StringUtils.removeStart(source.getAbsolutePath(), pb.getBasePath());
                    String translation = TranslationsUtils.replaceDoubleAsterisk(file.getSource(), file.getTranslation(), fileSource);
                    return placeholderUtil.replaceFileDependentPlaceholders(translation, source);
                })
                .flatMap(translation -> {
                    LanguageMapping localLanguageMapping = LanguageMapping.fromConfigFileLanguageMapping(file.getLanguagesMapping());
                    LanguageMapping languageMapping = LanguageMapping.populate(localLanguageMapping, projectLanguageMapping);
                    return language
                        .map(l -> Stream.of(placeholderUtil.replaceLanguageDependentPlaceholders(translation, languageMapping, l)))
                        .orElseGet(() -> placeholderUtil.replaceLanguageDependentPlaceholders(translation, languageMapping).stream());
                })
                .map(translation -> PropertiesBeanUtils.useTranslationReplace(translation, file.getTranslationReplace()))
            )
            .distinct()
            .filter(file -> (!filesMustExist) || new java.io.File(pb.getBasePath() + StringUtils.removeStart(file, Utils.PATH_SEPARATOR)).exists())
            .map(source -> StringUtils.removeStart(source, pb.getBasePath()))
            .collect(Collectors.toList());
    }
}
