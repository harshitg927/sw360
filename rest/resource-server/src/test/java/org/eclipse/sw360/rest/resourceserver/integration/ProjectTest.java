/*
 * Copyright Siemens AG, 2017-2018. Part of the SW360 Portal Project.
 *
  * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.integration;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.resourcelists.PaginationOptions;
import org.eclipse.sw360.datahandler.resourcelists.PaginationResult;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.project.Sw360ProjectService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
public class ProjectTest extends TestIntegrationBase {

    @Value("${local.server.port}")
    private int port;

    @MockBean
    private Sw360ProjectService projectServiceMock;

    @MockBean
    private RestControllerHelper restControllerHelperMock;

    @Before
    public void before() throws TException {
        // Setup project data
        Set<Project> projectSet = new HashSet<>();
        List<Project> projectList = new ArrayList<>();
        Project project = new Project();
        project.setName("Project name");
        project.setDescription("Project description");
        projectSet.add(project);
        projectList.add(project);

        // Mock original service methods
        given(this.projectServiceMock.getProjectsForUser(any(), any())).willReturn(projectSet);
        given(this.projectServiceMock.getProjectsSummaryForUserWithoutPagination(any())).willReturn(projectList);
        
        // Create a PaginationResult with the public constructor (takes only a list)
        PaginationResult<Project> paginationResult = new PaginationResult<>(projectList);
        
        // Mock the createPaginationResult method
        when(restControllerHelperMock.createPaginationResult(
            any(HttpServletRequest.class), 
            any(), 
            any(List.class), 
            any(String.class))).thenReturn(paginationResult);
        
        // Create a CollectionModel for the pagination result
        List<EntityModel<Project>> projectResources = new ArrayList<>();
        projectResources.add(EntityModel.of(project));
        CollectionModel<EntityModel<Project>> collectionModel = CollectionModel.of(projectResources);
        
        // Mock the new pagination resource method
        when(restControllerHelperMock.generatePagesResource(any(PaginationResult.class), any(List.class)))
            .thenReturn(collectionModel);
        
        // For empty resources case
        when(restControllerHelperMock.emptyPageResource(any(Class.class), any(PaginationResult.class)))
            .thenReturn(CollectionModel.empty());
        
        // Mock the user 
        User user = new User();
        user.setId("123456789");
        user.setEmail("admin@sw360.org");
        user.setFullname("John Doe");

        given(this.userServiceMock.getUserByEmailOrExternalId("admin@sw360.org")).willReturn(user);
        
        // Mock the project conversion methods
        when(restControllerHelperMock.convertToEmbeddedProject(any(Project.class))).thenReturn(project);
        when(restControllerHelperMock.getSw360UserFromAuthentication()).willReturn(user);
    }

    @Test
    public void should_get_all_projects() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        TestHelper.checkResponse(response.getBody(), "projects", 1);
    }
}
