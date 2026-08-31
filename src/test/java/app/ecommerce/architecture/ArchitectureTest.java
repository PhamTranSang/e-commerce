package app.ecommerce.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import jakarta.persistence.Entity;

@AnalyzeClasses(
    packages = "app.ecommerce",
    importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    /** An {@code api} package is a pure contract: it must never reach into any {@code impl}. */
    @ArchTest
    static final ArchRule api_must_not_depend_on_impl =
        noClasses().that().resideInAPackage("..api..")
            .should().dependOnClassesThat().resideInAPackage("..impl..");

    /** Shared contracts must not acquire dependencies on an individual business feature. */
    @ArchTest
    static final ArchRule shared_api_must_not_depend_on_business_features =
        noClasses().that().resideInAPackage("..shared.api..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..catalog..",
                "..product..",
                "..sku.."
            );

    /** Persistence entities are an implementation detail and must live inside {@code impl}. */
    @ArchTest
    static final ArchRule entities_live_only_in_impl =
        classes().that().areAnnotatedWith(Entity.class)
            .should().resideInAPackage("..impl..");

    /** Entities may cross feature boundaries, but only implementation code may reference them. */
    @ArchTest
    static final ArchRule entities_may_only_be_referenced_from_impl =
        noClasses().that().resideOutsideOfPackages("..impl..")
            .should().dependOnClassesThat().areAnnotatedWith(Entity.class);

    /** Modules must depend on each other in one direction only. */
    @ArchTest
    static final ArchRule modules_free_of_cycles =
        SlicesRuleDefinition.slices().matching("app.ecommerce.(*)..").should().beFreeOfCycles();
}