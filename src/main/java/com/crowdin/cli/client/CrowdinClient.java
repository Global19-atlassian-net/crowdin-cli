package com.crowdin.cli.client;

import com.crowdin.cli.commands.functionality.PropertiesBeanUtils;
import com.crowdin.cli.utils.Utils;
import com.crowdin.client.core.http.impl.json.JacksonJsonTransformer;
import com.crowdin.client.core.model.ClientConfig;
import com.crowdin.client.core.model.Credentials;
import com.crowdin.client.core.model.PatchRequest;
import com.crowdin.client.projectsgroups.model.ProjectSettings;
import com.crowdin.client.sourcefiles.model.AddBranchRequest;
import com.crowdin.client.sourcefiles.model.AddDirectoryRequest;
import com.crowdin.client.sourcefiles.model.AddFileRequest;
import com.crowdin.client.sourcefiles.model.Branch;
import com.crowdin.client.sourcefiles.model.Directory;
import com.crowdin.client.sourcefiles.model.UpdateFileRequest;
import com.crowdin.client.sourcestrings.model.AddSourceStringRequest;
import com.crowdin.client.sourcestrings.model.SourceString;
import com.crowdin.client.storage.model.Storage;
import com.crowdin.client.translations.model.BuildProjectTranslationRequest;
import com.crowdin.client.translations.model.ProjectBuild;
import com.crowdin.client.translations.model.UploadTranslationsRequest;
import com.crowdin.client.translationstatus.model.LanguageProgress;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

public class CrowdinClient extends CrowdinClientCore implements Client {

    private final com.crowdin.client.Client client;
    private final long projectId;

    public CrowdinClient(String apiToken, String baseUrl, long projectId) {
        boolean isTesting = PropertiesBeanUtils.isUrlForTesting(baseUrl);
        String organization = PropertiesBeanUtils.getOrganization(baseUrl);
        Credentials credentials = (isTesting)
            ? new Credentials(apiToken, organization, baseUrl)
            : new Credentials(apiToken, organization);
        ClientConfig clientConfig = ClientConfig.builder()
            .jsonTransformer(new JacksonJsonTransformer())
            .userAgent(Utils.buildUserAgent())
            .build();
        this.client = new com.crowdin.client.Client(credentials, clientConfig);
        this.projectId = projectId;
    }

    @Override
    public CrowdinProjectFull downloadFullProject() {
        CrowdinProjectFull project = new CrowdinProjectFull();
        this.populateProjectWithInfo(project);
        this.populateProjectWithLangs(project, project.getTargetLanguageIds());
        this.populateProjectWithStructure(project);
        return project;
    }

    @Override
    public CrowdinProject downloadProjectWithLanguages() {
        CrowdinProject project = new CrowdinProject();
        this.populateProjectWithInfo(project);
        this.populateProjectWithLangs(project, project.getTargetLanguageIds());
        return project;
    }

    @Override
    public CrowdinProjectInfo downloadProjectInfo() {
        CrowdinProjectInfo project = new CrowdinProjectInfo();
        this.populateProjectWithInfo(project);
        return project;
    }

    private void populateProjectWithStructure(CrowdinProjectFull project) {
        project.setFiles(executeRequestFullList((limit, offset) -> this.client.getSourceFilesApi()
            .listFiles(this.projectId, null, null, null, limit, offset)));
        project.setDirectories(executeRequestFullList((limit, offset) -> this.client.getSourceFilesApi()
            .listDirectories(this.projectId, null, null, null, limit, offset)));
        project.setBranches(executeRequestFullList((limit, offset) -> this.client.getSourceFilesApi()
            .listBranches(this.projectId, null, limit, offset)));
    }

    private void populateProjectWithLangs(CrowdinProject project, List<String> targetLanguageIds) {
        project.setSupportedLanguages(executeRequestFullList((limit, offset) -> this.client.getLanguagesApi()
            .listSupportedLanguages(limit, offset)));
        project.setProjectLanguages(project.getSupportedLanguages().stream()
            .filter(language -> targetLanguageIds.contains(language.getId()))
            .collect(Collectors.toList()));
    }

    private void populateProjectWithInfo(CrowdinProjectInfo project) {
        com.crowdin.client.projectsgroups.model.Project projectModel = this.getProject();
        project.setProjectId(projectModel.getId());
        project.setTargetLanguageIds(projectModel.getTargetLanguageIds());
        if (projectModel instanceof ProjectSettings) {
            project.setAccessLevel(CrowdinProjectInfo.Access.MANAGER);
            ProjectSettings projectSettings = (ProjectSettings) projectModel;
            if (projectSettings.isInContext()) {
                project.setInContextLanguageId(projectSettings.getInContextPseudoLanguageId());
            }
            project.setLanguageMapping(LanguageMapping.fromServerLanguageMapping(projectSettings.getLanguageMapping()));
        } else {
            project.setAccessLevel(CrowdinProjectInfo.Access.TRANSLATOR);
        }
    }

    private com.crowdin.client.projectsgroups.model.Project getProject() {
        return executeRequest(
            () -> (com.crowdin.client.projectsgroups.model.Project) this.client.getProjectsGroupsApi()
                .getProject(this.projectId)
                .getData());
    }

    @Override
    public List<LanguageProgress> getProjectProgress(String languageId) {
        return executeRequestFullList((limit, offset) -> this.client.getTranslationStatusApi()
            .getProjectProgress(this.projectId, limit, offset, languageId));
    }

    @Override
    public Branch addBranch(AddBranchRequest request) {
        return executeRequest(() -> this.client.getSourceFilesApi()
            .addBranch(this.projectId, request)
            .getData());
    }

    @Override
    public Long uploadStorage(String fileName, InputStream content) {
        Storage storage = executeRequest(() -> this.client.getStorageApi()
            .addStorage(fileName, content)
            .getData());
        return storage.getId();
    }

    @Override
    public Directory addDirectory(AddDirectoryRequest request) throws ResponseException {
        Map<BiPredicate<String, String>, ResponseException> errorHandlers = new LinkedHashMap<BiPredicate<String, String>, ResponseException>() {{
                put((code, message) -> StringUtils.containsAny(message, "Name must be unique", "This file is currently being updated"),
                    new ExistsResponseException());
                put((code, message) -> StringUtils.contains(message, "Already creating directory"),
                    new WaitResponseException());
            }};
        return executeRequest(errorHandlers, () -> this.client.getSourceFilesApi()
            .addDirectory(this.projectId, request)
            .getData());
    }

    @Override
    public void updateSource(Long sourceId, UpdateFileRequest request) {
        executeRequestWithPossibleRetry(
            (code, message) -> message.contains("File from storage with id #" + request.getStorageId() + " was not found"),
            () -> this.client.getSourceFilesApi()
                .updateOrRestoreFile(this.projectId, sourceId, request));
    }

    @Override
    public void addSource(AddFileRequest request) {
        executeRequestWithPossibleRetry(
            (code, message) -> message.contains("File from storage with id #" + request.getStorageId() + " was not found"),
            () -> this.client.getSourceFilesApi()
                .addFile(this.projectId, request));
    }

    @Override
    public void uploadTranslations(String languageId, UploadTranslationsRequest request) {
        executeRequestWithPossibleRetry(
            (code, message) -> message.contains("File from storage with id #" + request.getStorageId() + " was not found"),
            () -> this.client.getTranslationsApi()
                .uploadTranslations(this.projectId, languageId, request));
    }

    @Override
    public ProjectBuild startBuildingTranslation(BuildProjectTranslationRequest request) {
        return executeRequest(() -> this.client.getTranslationsApi()
            .buildProjectTranslation(this.projectId, request)
            .getData());
    }

    @Override
    public ProjectBuild checkBuildingTranslation(Long buildId) {
        return executeRequest(() -> this.client.getTranslationsApi()
            .checkBuildStatus(projectId, buildId)
            .getData());
    }

    @Override
    public InputStream downloadBuild(Long buildId) {
        String url = executeRequest(() -> this.client.getTranslationsApi()
            .downloadProjectTranslations(this.projectId, buildId)
            .getData()
            .getUrl());
        try {
            return new URL(url).openStream();
        } catch (IOException e) {
            throw new RuntimeException("Unexpected exception: malformed download url: " + url, e);
        }
    }

    @Override
    public SourceString addSourceString(AddSourceStringRequest request) {
        return executeRequest(() -> this.client.getSourceStringsApi()
            .addSourceString(this.projectId, request)
            .getData());
    }

    @Override
    public List<SourceString> listSourceString(Long fileId, String filter) {
        return executeRequestFullList((limit, offset) -> this.client.getSourceStringsApi()
            .listSourceStrings(this.projectId, fileId, filter, limit, offset));
    }

    @Override
    public void deleteSourceString(Long sourceId) {
        executeRequest(() -> {
            this.client.getSourceStringsApi()
                .deleteSourceString(this.projectId, sourceId);
            return true;
        });
    }

    @Override
    public SourceString editSourceString(Long sourceId, List<PatchRequest> requests) {
        return executeRequest(() -> this.client.getSourceStringsApi()
            .editSourceString(this.projectId, sourceId, requests)
            .getData());
    }
}
