package app.ecommerce.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import jakarta.persistence.Entity;

/**
 * Enforces the modular-monolith boundaries for the api-foundation milestone.
 *
 * <p>Business modules follow an {@code api}/{@code impl} package convention:
 * only {@code ..api..} is reachable across modules, {@code ..impl..} stays private.
 * These rules hard-fail on the first violation (no {@code FreezingArchRule}) because
 * the codebase starts clean and must stay at zero violations from day one.
 *
 * <p>The rules read thin today — no business module exists yet — but they are the
 * guardrail that trips the moment {@code catalog}/{@code identity} introduce
 * persistence types or cross-package coupling.
 */
@AnalyzeClasses(
    packages = "app.ecommerce",
    importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    /** An {@code api} package is a pure contract: it must never reach into any {@code impl}. */
    @ArchTest
    static final ArchRule api_must_not_depend_on_impl =
        noClasses().that().resideInAPackage("..api..")
            .should().dependOnClassesThat().resideInAPackage("..impl..");

    /** Persistence entities are an implementation detail and must live inside {@code impl}. */
    @ArchTest
    static final ArchRule entities_live_only_in_impl =
        classes().that().areAnnotatedWith(Entity.class)
            .should().resideInAPackage("..impl..");

    /** Nothing outside {@code impl} may reference a persistence entity. */
    @ArchTest
    static final ArchRule entities_never_referenced_outside_impl =
        noClasses().that().resideOutsideOfPackages("..impl..")
            .should().dependOnClassesThat().areAnnotatedWith(Entity.class);

    /** Modules must depend on each other in one direction only. */
    @ArchTest
    static final ArchRule modules_free_of_cycles =
        SlicesRuleDefinition.slices().matching("app.ecommerce.(*)..").should().beFreeOfCycles();
}
