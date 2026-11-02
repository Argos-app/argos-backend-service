package com.argos.backend_services.repository;

import com.argos.backend_services.entity.AppUser;
import com.argos.backend_services.entity.Permission;
import com.argos.backend_services.entity.Property;
import com.argos.backend_services.entity.UserProperty;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
class UserGroupingRepositoryTest {
    @Autowired private EntityManager em;
    @Autowired private UserRepository users;
    @Autowired private UserPropertyRepository links;
    @Autowired private PropertyRepository properties;
    private AppUser administrator;
    private AppUser firstUser;
    private AppUser secondUser;
    private Permission permission;
    private int sequence;

    @BeforeEach
    void setUp() {
        sequence = 0;
        permission = Permission.builder().code("ADMIN").name("Administrator").active(true).build();
        em.persist(permission);
        administrator = user("Owner A");
        AppUser otherAdministrator = user("Owner B");
        firstUser = user("A One");
        secondUser = user("A Two");
        AppUser foreignUser = user("Foreign User");
        Property alpha = farm("Alpha", administrator, true);
        Property zeta = farm("Zeta", administrator, true);
        Property foreign = farm("Foreign", otherAdministrator, true);
        Property inactive = farm("Inactive", administrator, false);
        link(firstUser, alpha);
        link(firstUser, zeta);
        link(firstUser, foreign);
        link(firstUser, inactive);
        link(secondUser, zeta);
        link(foreignUser, foreign);
        em.flush();
        em.clear();
    }

    @Test
    void shouldResolveOnlyActiveOwnedFarmsForReportExport() {
        assertThat(properties.findByUserIdAndActiveTrue(administrator.getId()))
                .extracting(Property::getName).containsExactlyInAnyOrder("Alpha", "Zeta");
        assertThat(properties.findByUserIdAndActiveTrue(firstUser.getId())).isEmpty();
    }

    @Test
    void shouldPaginateUniqueUsersBeforeLoadingProperties() {
        var firstPage = users.findLinkedByAdministrator(administrator.getId(), PageRequest.of(0, 1));
        var secondPage = users.findLinkedByAdministrator(administrator.getId(), PageRequest.of(1, 1));

        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.getContent()).extracting(item -> item.userId()).containsExactly(firstUser.getId());
        assertThat(secondPage.getContent()).extracting(item -> item.userId()).containsExactly(secondUser.getId());
    }

    @Test
    void shouldLoadAllOwnedActiveFarmsOnlyForRequestedUsersInAlphabeticalOrder() {
        var result = links.findActiveLinksByUsersAndAdministrator(List.of(firstUser.getId()), administrator.getId());

        assertThat(result).extracting(link -> link.getProperty().getName()).containsExactly("Alpha", "Zeta");
        assertThat(result).allMatch(link -> link.getUser().getId().equals(firstUser.getId()));
    }

    @Test
    void shouldLoadProfileFarmsAcrossOwnersAndExcludeInactiveLinksAndFarms() {
        Property unlinked = farm("Unlinked", administrator, true);
        em.persist(UserProperty.builder().user(em.getReference(AppUser.class, firstUser.getId()))
                .property(unlinked).linkDate(LocalDate.of(2026, 1, 15)).active(false).build());
        em.flush();
        em.clear();

        var result = links.findActiveProfileProperties(firstUser.getId());

        assertThat(result).extracting(item -> item.name()).containsExactly("Alpha", "Foreign", "Zeta");
        assertThat(result).extracting(item -> item.id()).doesNotHaveDuplicates();
        assertThat(links.findActiveProfileProperties(secondUser.getId()))
                .extracting(item -> item.name()).containsExactly("Zeta");
        assertThat(links.findActiveProfileProperties(administrator.getId())).isEmpty();
    }

    @Test
    void shouldFetchCurrentProfileAndPermissionWithoutRequiringFarmLinks() {
        var result = users.findActiveProfileById(administrator.getId()).orElseThrow();

        assertThat(result.getName()).isEqualTo("Owner A");
        assertThat(em.getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(result, "permission")).isTrue();
        assertThat(result.getPermission().getName()).isEqualTo("Administrator");
        assertThat(users.findActiveProfileById(java.util.UUID.randomUUID())).isEmpty();
    }

    @Test
    void shouldRejectInactiveAccountsInProfileQuery() {
        em.find(AppUser.class, firstUser.getId()).setActive(false);
        em.flush();
        em.clear();

        assertThat(users.findActiveProfileById(firstUser.getId())).isEmpty();
    }

    @Test
    void shouldRejectInactivePermissionsInProfileQuery() {
        em.find(Permission.class, permission.getId()).setActive(false);
        em.flush();
        em.clear();

        assertThat(users.findActiveProfileById(firstUser.getId())).isEmpty();
    }

    @Test
    void shouldKeepInactiveLinkedAccountsInUsageListing() {
        em.find(AppUser.class, firstUser.getId()).setActive(false);
        em.flush();
        em.clear();

        var result = users.findLinkedByAdministrator(administrator.getId(), PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).extracting(item -> item.userId())
                .containsExactly(firstUser.getId(), secondUser.getId());
        assertThat(result.getContent().getFirst().active()).isFalse();
    }

    @Test
    void shouldExcludeUsersWithOnlyInactiveLinksOrInactiveFarms() {
        var inactiveLinkUser = user("Inactive link user");
        var inactiveFarmUser = user("Inactive farm user");
        var activeFarm = farm("New active", administrator, true);
        var inactiveFarm = farm("New inactive", administrator, false);
        em.persist(UserProperty.builder().user(inactiveLinkUser).property(activeFarm)
                .linkDate(LocalDate.of(2026, 1, 15)).active(false).build());
        link(inactiveFarmUser, inactiveFarm);
        em.flush();
        em.clear();

        var result = users.findLinkedByAdministrator(administrator.getId(), PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).extracting(item -> item.userId())
                .containsExactly(firstUser.getId(), secondUser.getId());
    }

    @Test
    void shouldUseIdToBreakNameTiesAcrossPagesWithoutAddingTheAdministrator() {
        em.find(AppUser.class, firstUser.getId()).setName("Same name");
        em.find(AppUser.class, secondUser.getId()).setName("Same name");
        em.flush();
        em.clear();

        var expected = List.of(firstUser.getId(), secondUser.getId()).stream()
                .sorted(java.util.Comparator.comparing(java.util.UUID::toString)).toList();
        var firstPage = users.findLinkedByAdministrator(administrator.getId(), PageRequest.of(0, 1));
        var secondPage = users.findLinkedByAdministrator(administrator.getId(), PageRequest.of(1, 1));
        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(firstPage.getContent().getFirst().userId()).isEqualTo(expected.getFirst());
        assertThat(secondPage.getContent().getFirst().userId()).isEqualTo(expected.get(1));
    }

    private AppUser user(String name) {
        var value = AppUser.builder().name(name).email("grouping" + (++sequence) + "@test.invalid")
                .cpf("%011d".formatted(sequence)).permission(permission).active(true).build();
        em.persist(value);
        return value;
    }

    private Property farm(String name, AppUser owner, boolean active) {
        var value = Property.builder().name(name).user(owner).address("Test Street").city("Test City")
                .state("SP").active(active).build();
        em.persist(value);
        return value;
    }

    private void link(AppUser user, Property farm) {
        em.persist(UserProperty.builder().user(user).property(farm).linkDate(LocalDate.of(2026, 1, 15))
                .active(true).build());
    }
}
