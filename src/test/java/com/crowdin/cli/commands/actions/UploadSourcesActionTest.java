package com.crowdin.cli.commands.actions;

import com.crowdin.cli.client.Client;
import com.crowdin.cli.client.ProjectBuilder;
import com.crowdin.cli.client.ResponseException;
import com.crowdin.cli.client.models.BranchBuilder;
import com.crowdin.cli.client.models.DirectoryBuilder;
import com.crowdin.cli.commands.ClientAction;
import com.crowdin.cli.commands.Outputter;
import com.crowdin.cli.properties.PropertiesBean;
import com.crowdin.cli.properties.PropertiesBeanBuilder;
import com.crowdin.cli.properties.helper.FileHelperTest;
import com.crowdin.cli.properties.helper.TempProject;
import com.crowdin.cli.utils.Utils;
import com.crowdin.client.sourcefiles.model.AddBranchRequest;
import com.crowdin.client.sourcefiles.model.AddDirectoryRequest;
import com.crowdin.client.sourcefiles.model.AddFileRequest;
import com.crowdin.client.sourcefiles.model.Branch;
import com.crowdin.client.sourcefiles.model.Directory;
import com.crowdin.client.sourcefiles.model.OtherFileImportOptions;
import com.crowdin.client.sourcefiles.model.PropertyFileExportOptions;
import com.crowdin.client.sourcefiles.model.SpreadsheetFileImportOptions;
import com.crowdin.client.sourcefiles.model.UpdateFileRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class UploadSourcesActionTest {

    private TempProject project;

    @BeforeEach
    public void createProj() {
        project = new TempProject(FileHelperTest.class);
    }

    @AfterEach
    public void deleteProj() {
        project.delete();
    }

    @Test
    public void testUploadOneSource_EmptyProject() throws ResponseException {
        project.addFile(Utils.normalizePath("first.po"), "Hello, World!");
        PropertiesBeanBuilder pbBuilder = PropertiesBeanBuilder
            .minimalBuiltPropertiesBean("*", Utils.PATH_SEPARATOR + "%original_file_name%-CR-%locale%")
            .setBasePath(project.getBasePath());
        PropertiesBean pb = pbBuilder.build();
        Client client = mock(Client.class);
        when(client.downloadFullProject())
            .thenReturn(ProjectBuilder.emptyProject(Long.parseLong(pb.getProjectId())).build());
        when(client.uploadStorage(eq("first.po"), any()))
            .thenReturn(1L);

        ClientAction action = new UploadSourcesAction(null, false, true, false, false);
        action.act(Outputter.getDefault(), pb, client);

        verify(client).downloadFullProject();
        verify(client).uploadStorage(eq("first.po"), any());
        AddFileRequest addFileRequest = new AddFileRequest() {{
                setName("first.po");
                setStorageId(1L);
                setImportOptions(new OtherFileImportOptions() {{
                        setContentSegmentation(pb.getFiles().get(0).getContentSegmentation());
                    }}
                );
                setExportOptions(new PropertyFileExportOptions() {{
                        setEscapeQuotes(pb.getFiles().get(0).getEscapeQuotes());
                        setExportPattern(pb.getFiles().get(0).getTranslation().replaceAll("[\\\\/]+", "/"));
                    }}
                );
            }};
        verify(client).addSource(eq(addFileRequest));
        verifyNoMoreInteractions(client);
    }

    @Test
    public void testUploadFewSourceWithDirectories_EmptyProject() throws ResponseException {
        project.addFile(Utils.normalizePath("first.po"), "Hello, World!");
        project.addFile(Utils.normalizePath("folder/second.po"), "Hello, World!");
        project.addFile(Utils.normalizePath("third.po"), "Hello, World!");
        PropertiesBeanBuilder pbBuilder = PropertiesBeanBuilder
            .minimalBuiltPropertiesBean(Utils.normalizePath("**/*"), Utils.PATH_SEPARATOR + "%original_file_name%-CR-%locale%")
            .setBasePath(project.getBasePath());
        PropertiesBean pb = pbBuilder.build();
        Client client = mock(Client.class);
        when(client.downloadFullProject())
            .thenReturn(ProjectBuilder.emptyProject(Long.parseLong(pb.getProjectId())).build());
        when(client.uploadStorage(eq("first.po"), any()))
            .thenReturn(1L);
        AddDirectoryRequest addDirectoryRequest = new AddDirectoryRequest() {{
                setName("folder");
            }};
        Directory directory = DirectoryBuilder.standard().setProjectId(Long.parseLong(pb.getProjectId()))
            .setIdentifiers("folder", 201L, null, null).build();
        when(client.addDirectory(eq(addDirectoryRequest)))
            .thenReturn(directory);
        when(client.uploadStorage(eq("second.po"), any()))
            .thenReturn(2L);
        when(client.uploadStorage(eq("third.po"), any()))
            .thenReturn(3L);

        ClientAction action = new UploadSourcesAction(null, false, true, false, false);
        action.act(Outputter.getDefault(), pb, client);

        verify(client).downloadFullProject();
        verify(client).uploadStorage(eq("first.po"), any());
        verify(client).addDirectory(eq(addDirectoryRequest));
        verify(client).uploadStorage(eq("second.po"), any());
        verify(client).uploadStorage(eq("third.po"), any());
        AddFileRequest addFileRequest1 = new AddFileRequest() {{
                setName("first.po");
                setStorageId(1L);
                setImportOptions(new OtherFileImportOptions() {{
                        setContentSegmentation(pb.getFiles().get(0).getContentSegmentation());
                    }}
                );
                setExportOptions(new PropertyFileExportOptions() {{
                        setEscapeQuotes(pb.getFiles().get(0).getEscapeQuotes());
                        setExportPattern(pb.getFiles().get(0).getTranslation().replaceAll("[\\\\/]+", "/"));
                    }}
                );
            }};
        verify(client).addSource(eq(addFileRequest1));
        AddFileRequest addFileRequest2 = new AddFileRequest() {{
                setName("second.po");
                setStorageId(2L);
                setDirectoryId(201L);
                setImportOptions(new OtherFileImportOptions() {{
                        setContentSegmentation(pb.getFiles().get(0).getContentSegmentation());
                    }}
                );
                setExportOptions(new PropertyFileExportOptions() {{
                        setEscapeQuotes(pb.getFiles().get(0).getEscapeQuotes());
                        setExportPattern(pb.getFiles().get(0).getTranslation().replaceAll("[\\\\/]+", "/"));
                    }}
                );
            }};
        verify(client).addSource(eq(addFileRequest2));
        AddFileRequest addFileRequest3 = new AddFileRequest() {{
                setName("third.po");
                setStorageId(3L);
                setImportOptions(new OtherFileImportOptions() {{
                        setContentSegmentation(pb.getFiles().get(0).getContentSegmentation());
                    }}
                );
                setExportOptions(new PropertyFileExportOptions() {{
                        setEscapeQuotes(pb.getFiles().get(0).getEscapeQuotes());
                        setExportPattern(pb.getFiles().get(0).getTranslation().replaceAll("[\\\\/]+", "/"));
                    }}
                );
            }};
        verify(client).addSource(eq(addFileRequest3));
        verifyNoMoreInteractions(client);
    }

    @Test
    public void testUploadOneSourceWithBranch_EmptyProject() throws ResponseException {
        project.addFile(Utils.normalizePath("first.po"), "Hello, World!");
        PropertiesBeanBuilder pbBuilder = PropertiesBeanBuilder
            .minimalBuiltPropertiesBean("*", Utils.PATH_SEPARATOR + "%original_file_name%-CR-%locale%")
            .setBasePath(project.getBasePath());
        PropertiesBean pb = pbBuilder.build();
        Client client = mock(Client.class);
        when(client.downloadFullProject())
            .thenReturn(ProjectBuilder.emptyProject(Long.parseLong(pb.getProjectId())).build());
        when(client.uploadStorage(eq("first.po"), any()))
            .thenReturn(1L);
        Branch branch = BranchBuilder.standard().setProjectId(Long.parseLong(pb.getProjectId()))
            .setIdentifiers("newBranch", 201L).build();
        AddBranchRequest addBranchRequest = new AddBranchRequest() {{
                setName("newBranch");
            }};
        when(client.addBranch(addBranchRequest))
            .thenReturn(branch);

        ClientAction action = new UploadSourcesAction("newBranch", false, true, false, false);
        action.act(Outputter.getDefault(), pb, client);

        verify(client).downloadFullProject();
        verify(client).addBranch(addBranchRequest);
        verify(client).uploadStorage(eq("first.po"), any());
        AddFileRequest addFileRequest = new AddFileRequest() {{
                setName("first.po");
                setStorageId(1L);
                setBranchId(201L);
                setImportOptions(new OtherFileImportOptions() {{
                        setContentSegmentation(pb.getFiles().get(0).getContentSegmentation());
                    }}
                );
                setExportOptions(new PropertyFileExportOptions() {{
                        setEscapeQuotes(pb.getFiles().get(0).getEscapeQuotes());
                        setExportPattern(pb.getFiles().get(0).getTranslation().replaceAll("[\\\\/]+", "/"));
                    }}
                );
            }};
        verify(client).addSource(eq(addFileRequest));
        verifyNoMoreInteractions(client);
    }

    @Test
    public void testUploadOneSourceWithBranch_ProjectWithThatBranch() throws ResponseException {
        project.addFile(Utils.normalizePath("first.po"), "Hello, World!");
        PropertiesBeanBuilder pbBuilder = PropertiesBeanBuilder
            .minimalBuiltPropertiesBean("*", Utils.PATH_SEPARATOR + "%original_file_name%-CR-%locale%")
            .setBasePath(project.getBasePath());
        PropertiesBean pb = pbBuilder.build();
        Client client = mock(Client.class);
        when(client.downloadFullProject())
            .thenReturn(ProjectBuilder.emptyProject(Long.parseLong(pb.getProjectId()))
                .addBranches(201L, "newBranch").build());
        when(client.uploadStorage(eq("first.po"), any()))
            .thenReturn(1L);

        ClientAction action = new UploadSourcesAction("newBranch", false, true, false, false);
        action.act(Outputter.getDefault(), pb, client);

        verify(client).downloadFullProject();
        verify(client).uploadStorage(eq("first.po"), any());
        AddFileRequest addFileRequest = new AddFileRequest() {{
                setName("first.po");
                setStorageId(1L);
                setBranchId(201L);
                setImportOptions(new OtherFileImportOptions() {{
                        setContentSegmentation(pb.getFiles().get(0).getContentSegmentation());
                    }}
                );
                setExportOptions(new PropertyFileExportOptions() {{
                        setEscapeQuotes(pb.getFiles().get(0).getEscapeQuotes());
                        setExportPattern(pb.getFiles().get(0).getTranslation().replaceAll("[\\\\/]+", "/"));
                    }}
                );
            }};
        verify(client).addSource(eq(addFileRequest));
        verifyNoMoreInteractions(client);
    }

    @Test
    public void testUploadOneSourceWithDirectory_ProjectNotPreserveHierarchy() throws ResponseException {
        project.addFile(Utils.normalizePath("folder/first.po"), "Hello, World!");
        PropertiesBeanBuilder pbBuilder = PropertiesBeanBuilder
            .minimalBuiltPropertiesBean(Utils.normalizePath("**/*"), Utils.PATH_SEPARATOR + "%original_file_name%-CR-%locale%")
            .setBasePath(project.getBasePath());
        PropertiesBean pb = pbBuilder.build();
        Client client = mock(Client.class);
        when(client.downloadFullProject())
            .thenReturn(ProjectBuilder.emptyProject(Long.parseLong(pb.getProjectId())).build());
        when(client.uploadStorage(eq("first.po"), any()))
            .thenReturn(1L);

        ClientAction action = new UploadSourcesAction(null, false, true, false, false);
        action.act(Outputter.getDefault(), pb, client);

        verify(client).downloadFullProject();
        verify(client).uploadStorage(eq("first.po"), any());
        AddFileRequest addFileRequest = new AddFileRequest() {{
                setName("first.po");
                setStorageId(1L);
                setDirectoryId(null);
                setImportOptions(new OtherFileImportOptions() {{
                        setContentSegmentation(pb.getFiles().get(0).getContentSegmentation());
                    }}
                );
                setExportOptions(new PropertyFileExportOptions() {{
                        setEscapeQuotes(pb.getFiles().get(0).getEscapeQuotes());
                        setExportPattern(pb.getFiles().get(0).getTranslation().replaceAll("[\\\\/]+", "/"));
                    }}
                );
            }};
        verify(client).addSource(eq(addFileRequest));
        verifyNoMoreInteractions(client);
    }

    @Test
    public void testUploadOneSourceWithDirectory_ProjectWithPreserveHierarchy() throws ResponseException {
        project.addFile(Utils.normalizePath("folder/first.po"), "Hello, World!");
        PropertiesBeanBuilder pbBuilder = PropertiesBeanBuilder
            .minimalBuiltPropertiesBean(Utils.normalizePath("**/*"), Utils.PATH_SEPARATOR + "%original_file_name%-CR-%locale%")
            .setBasePath(project.getBasePath());
        PropertiesBean pb = pbBuilder.build();
        pb.setPreserveHierarchy(true);
        Client client = mock(Client.class);
        when(client.downloadFullProject())
            .thenReturn(ProjectBuilder.emptyProject(Long.parseLong(pb.getProjectId()))
                .addDirectory("folder", 101L, null, null).build());
        when(client.uploadStorage(eq("first.po"), any()))
            .thenReturn(1L);

        ClientAction action = new UploadSourcesAction(null, false, true, false, false);
        action.act(Outputter.getDefault(), pb, client);

        verify(client).downloadFullProject();
        verify(client).uploadStorage(eq("first.po"), any());
        AddFileRequest addFileRequest = new AddFileRequest() {{
                setName("first.po");
                setStorageId(1L);
                setDirectoryId(101L);
                setImportOptions(new OtherFileImportOptions() {{
                        setContentSegmentation(pb.getFiles().get(0).getContentSegmentation());
                    }}
                );
                setExportOptions(new PropertyFileExportOptions() {{
                        setEscapeQuotes(pb.getFiles().get(0).getEscapeQuotes());
                        setExportPattern(pb.getFiles().get(0).getTranslation().replaceAll("[\\\\/]+", "/"));
                    }}
                );
            }};
        verify(client).addSource(eq(addFileRequest));
        verifyNoMoreInteractions(client);
    }

    @Test
    public void testUploadOneSourceWithDest_Project() throws ResponseException {
        project.addFile(Utils.normalizePath("first.po"), "Hello, World!");
        PropertiesBeanBuilder pbBuilder = PropertiesBeanBuilder
            .minimalBuiltPropertiesBean(Utils.normalizePath("first.po"), Utils.PATH_SEPARATOR + "%original_file_name%-CR-%locale%")
            .setBasePath(project.getBasePath());
        PropertiesBean pb = pbBuilder.build();
        pb.getFiles().get(0).setDest("last.po");
        Client client = mock(Client.class);
        when(client.downloadFullProject())
            .thenReturn(ProjectBuilder.emptyProject(Long.parseLong(pb.getProjectId())).build());
        when(client.uploadStorage(eq("last.po"), any()))
            .thenReturn(1L);

        ClientAction action = new UploadSourcesAction(null, false, true, false, false);
        action.act(Outputter.getDefault(), pb, client);

        verify(client).downloadFullProject();
        verify(client).uploadStorage(eq("last.po"), any());
        AddFileRequest addFileRequest = new AddFileRequest() {{
                setName("last.po");
                setStorageId(1L);
                setDirectoryId(null);
                setImportOptions(new OtherFileImportOptions() {{
                        setContentSegmentation(pb.getFiles().get(0).getContentSegmentation());
                    }}
                );
                setExportOptions(new PropertyFileExportOptions() {{
                        setEscapeQuotes(pb.getFiles().get(0).getEscapeQuotes());
                        setExportPattern(pb.getFiles().get(0).getTranslation().replaceAll("[\\\\/]+", "/"));
                    }}
                );
            }};
        verify(client).addSource(eq(addFileRequest));
        verifyNoMoreInteractions(client);
    }

    @Test
    public void testUpdateOneUploadOneSource_Project() throws ResponseException {
        project.addFile(Utils.normalizePath("first.po"), "Hello, World!");
        project.addFile(Utils.normalizePath("second.po"), "Hello, World!");
        PropertiesBeanBuilder pbBuilder = PropertiesBeanBuilder
            .minimalBuiltPropertiesBean(Utils.normalizePath("*"), Utils.PATH_SEPARATOR + "%original_file_name%-CR-%locale%")
            .setBasePath(project.getBasePath());
        PropertiesBean pb = pbBuilder.build();
        Client client = mock(Client.class);
        when(client.downloadFullProject())
            .thenReturn(ProjectBuilder.emptyProject(Long.parseLong(pb.getProjectId()))
                .addFile("first.po", "gettext", 101L, null, null).build());
        when(client.uploadStorage(eq("first.po"), any()))
            .thenReturn(1L);
        when(client.uploadStorage(eq("second.po"), any()))
            .thenReturn(2L);

        ClientAction action = new UploadSourcesAction(null, false, true, false, false);
        action.act(Outputter.getDefault(), pb, client);

        verify(client).downloadFullProject();
        verify(client).uploadStorage(eq("first.po"), any());
        verify(client).uploadStorage(eq("second.po"), any());
        UpdateFileRequest updateFileRequest = new UpdateFileRequest() {{
                setStorageId(1L);
                setImportOptions(new OtherFileImportOptions() {{
                        setContentSegmentation(pb.getFiles().get(0).getContentSegmentation());
                    }}
                );
                setExportOptions(new PropertyFileExportOptions() {{
                        setEscapeQuotes(pb.getFiles().get(0).getEscapeQuotes());
                        setExportPattern(pb.getFiles().get(0).getTranslation().replaceAll("[\\\\/]+", "/"));
                    }}
                );
            }};
        verify(client).updateSource(eq(101L), eq(updateFileRequest));
        AddFileRequest addFileRequest = new AddFileRequest() {{
                setName("second.po");
                setStorageId(2L);
                setImportOptions(new OtherFileImportOptions() {{
                        setContentSegmentation(pb.getFiles().get(0).getContentSegmentation());
                    }}
                );
                setExportOptions(new PropertyFileExportOptions() {{
                        setEscapeQuotes(pb.getFiles().get(0).getEscapeQuotes());
                        setExportPattern(pb.getFiles().get(0).getTranslation().replaceAll("[\\\\/]+", "/"));
                    }}
                );
            }};
        verify(client).addSource(eq(addFileRequest));
        verifyNoMoreInteractions(client);
    }

    @Test
    public void testAddCsvFile_EmptyProject() throws ResponseException {

        project.addFile(Utils.normalizePath("first.csv"), "Hello, World!");
        PropertiesBeanBuilder pbBuilder = PropertiesBeanBuilder
                .minimalBuiltPropertiesBean("*", Utils.PATH_SEPARATOR + "%original_file_name%-CR-%locale%")
                .setBasePath(project.getBasePath());
        PropertiesBean pb = pbBuilder.build();
        pb.getFiles().get(0).setScheme("identifier,source_phrase,context,uk,ru,fr");
        Client client = mock(Client.class);
        when(client.downloadFullProject())
                .thenReturn(ProjectBuilder.emptyProject(Long.parseLong(pb.getProjectId())).build());
        when(client.uploadStorage(eq("first.csv"), any()))
                .thenReturn(1L);

        ClientAction action = new UploadSourcesAction(null, false, true, false, false);
        action.act(Outputter.getDefault(), pb, client);

        verify(client).downloadFullProject();
        verify(client).uploadStorage(eq("first.csv"), any());
        Map<String, Integer> scheme = new HashMap<>();
        scheme.put("identifier", 0);
        scheme.put("source_phrase", 1);
        scheme.put("context", 2);
        scheme.put("uk", 3);
        scheme.put("ru", 4);
        scheme.put("fr", 5);
        AddFileRequest addFileRequest = new AddFileRequest() {{
                setName("first.csv");
                setStorageId(1L);
                setImportOptions(new SpreadsheetFileImportOptions() {{
                        setScheme(scheme);
                        setFirstLineContainsHeader(false);
                    }}
                );
                setExportOptions(new PropertyFileExportOptions() {{
                        setEscapeQuotes(pb.getFiles().get(0).getEscapeQuotes());
                        setExportPattern(pb.getFiles().get(0).getTranslation().replaceAll("[\\\\/]+", "/"));
                    }}
                );
            }};
        verify(client).addSource(eq(addFileRequest));
        verifyNoMoreInteractions(client);
    }
}
