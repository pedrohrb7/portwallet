package com.portwallet;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.data.repository.Repository;

@AnalyzeClasses(packages = "com.portwallet", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule controllersDoNotUseRepositories = noClasses()
            .that().resideInAPackage("..api..")
            .should().dependOnClassesThat().areAssignableTo(Repository.class)
            .orShould().dependOnClassesThat().resideInAPackage("..infra..")
            .because("controllers delegate to the application layer (docs/convencoes.md)")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule domainDoesNotDependOnOuterLayers = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("..api..", "..infra..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule sharedDoesNotDependOnFeatures = noClasses()
            .that().resideInAPackage("com.portwallet.shared..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.portwallet.auth..", "com.portwallet.account..", "com.portwallet.ledger..",
                    "com.portwallet.transfer..", "com.portwallet.mock..")
            .allowEmptyShould(true);
}
