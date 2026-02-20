package app.partsvibe.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class ArchitectureTest {
    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("app.partsvibe");

    @Test
    void web_should_not_access_repo() {
        noClasses()
                .that()
                .resideInAnyPackage("..web..")
                .should()
                .accessClassesThat()
                .resideInAnyPackage("..repo..")
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    @Test
    void web_should_not_access_domain() {
        noClasses()
                .that()
                .resideInAnyPackage("..web..")
                .should()
                .accessClassesThat()
                .resideInAnyPackage("..domain..")
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    @Test
    void service_should_not_access_web() {
        noClasses()
                .that()
                .resideInAnyPackage("..service..")
                .should()
                .accessClassesThat()
                .resideInAnyPackage("..web..")
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    @Test
    void repo_should_not_access_web_or_service() {
        noClasses()
                .that()
                .resideInAnyPackage("..repo..")
                .should()
                .accessClassesThat()
                .resideInAnyPackage("..web..", "..service..")
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    @Test
    void domain_should_be_independent() {
        noClasses()
                .that()
                .resideInAnyPackage("..domain..")
                .should()
                .accessClassesThat()
                .resideInAnyPackage("..web..", "..service..", "..repo..")
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    @Test
    void modules_should_not_use_search_impl_directly() {
        noClasses()
                .that()
                .resideInAnyPackage("..catalog..", "..users..")
                .should()
                .accessClassesThat()
                .resideInAnyPackage("..search.solr..")
                .allowEmptyShould(true)
                .check(CLASSES);
    }
}
