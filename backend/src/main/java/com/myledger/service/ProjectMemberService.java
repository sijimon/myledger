package com.myledger.service;

import com.myledger.entity.ProjectMember;
import com.myledger.entity.Role;
import com.myledger.entity.User;
import com.myledger.repository.ProjectMemberRepository;
import com.myledger.repository.ProjectRepository;
import com.myledger.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Manages which projects a contractor is assigned to. */
@Service
public class ProjectMemberService {

    private final ProjectMemberRepository members;
    private final ProjectRepository projects;
    private final UserRepository users;

    public ProjectMemberService(ProjectMemberRepository members, ProjectRepository projects, UserRepository users) {
        this.members = members;
        this.projects = projects;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<Long> assignedProjectIds(Long userId) {
        return members.findProjectIdsByUserId(userId);
    }

    public boolean isAssigned(Long userId, Long projectId) {
        return members.existsByProjectIdAndUserId(projectId, userId);
    }

    @Transactional
    public List<Long> setAssignments(Long userId, List<Long> projectIds) {
        User user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getRole() != Role.ROLE_CONTRACTOR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only contractors can be assigned to projects");
        }
        List<Long> ids = projectIds == null ? List.of() : projectIds.stream().distinct().toList();
        for (Long pid : ids) {
            if (!projects.existsById(pid)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown project: " + pid);
            }
        }
        members.deleteByUserId(userId);
        for (Long pid : ids) {
            members.save(new ProjectMember(pid, userId));
        }
        return ids;
    }
}
