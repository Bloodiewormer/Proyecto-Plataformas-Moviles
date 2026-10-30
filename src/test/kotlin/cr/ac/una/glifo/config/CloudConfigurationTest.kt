package cr.ac.una.glifo.config

import cr.ac.una.glifo.ops.config.FcmProperties
import cr.ac.una.glifo.ops.fcm.FcmErrorKind
import cr.ac.una.glifo.ops.fcm.FcmSendOutcome
import cr.ac.una.glifo.ops.fcm.FcmTargetedMessage
import cr.ac.una.glifo.ops.fcm.FirebaseAdminFcmClient
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver
import org.springframework.boot.context.properties.source.ConfigurationPropertySources
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.MutablePropertySources
import org.springframework.core.env.PropertySourcesPropertyResolver
import org.springframework.core.io.support.ResourcePropertySource
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CloudConfigurationTest {
    private val deploymentVariables = mapOf<String, Any>(
        "DB_JDBC_URL" to "jdbc:postgresql://database.invalid:5432/glifo?sslmode=verify-full",
        "DB_USERNAME" to "cloud-user",
        "DB_PASSWORD" to "fixture-password",
        "JWT_SECRET" to "fixture-only-signing-key-never-for-deployment",
        "CORS_ALLOWED_ORIGIN" to "https://client.invalid"
    )

    private fun propertySources(
        variables: Map<String, Any> = emptyMap(),
        includeDevelopment: Boolean = true
    ): MutablePropertySources {
        val sources = MutablePropertySources()
        sources.addFirst(MapPropertySource("deployment-variables", variables))
        sources.addLast(ResourcePropertySource("classpath:application-cloud.properties"))
        if (includeDevelopment) {
            sources.addLast(ResourcePropertySource("classpath:application.properties"))
        }
        return sources
    }

    private fun resolver(
        variables: Map<String, Any> = emptyMap(),
        includeDevelopment: Boolean = true
    ) = PropertySourcesPropertyResolver(propertySources(variables, includeDevelopment))

    private fun bindDatasource(variables: Map<String, Any>): Map<String, String> {
        val sources = propertySources(variables)
        val binder = Binder(ConfigurationPropertySources.from(sources), PropertySourcesPlaceholdersResolver(sources))
        return binder.bind("spring.datasource", Bindable.mapOf(String::class.java, String::class.java)).get()
    }

    @Test
    fun cloudOverridesDestructiveDevelopmentDefaults() {
        val properties = resolver(deploymentVariables)
        assertEquals("validate", properties.getRequiredProperty("spring.jpa.hibernate.ddl-auto"))
        assertEquals("never", properties.getRequiredProperty("spring.sql.init.mode"))
        assertEquals("false", properties.getRequiredProperty("spring.jpa.show-sql"))
        assertEquals("never", properties.getRequiredProperty("server.error.include-message"))
        assertEquals("never", properties.getRequiredProperty("server.error.include-stacktrace"))
    }

    @Test
    fun deploymentValuesAreResolvedWithoutDevelopmentCredentials() {
        val properties = resolver(deploymentVariables, includeDevelopment = false)
        assertEquals(deploymentVariables["DB_JDBC_URL"], properties.getRequiredProperty("spring.datasource.url"))
        assertEquals(deploymentVariables["DB_USERNAME"], properties.getRequiredProperty("spring.datasource.username"))
        assertEquals(deploymentVariables["DB_PASSWORD"], properties.getRequiredProperty("spring.datasource.password"))
        assertEquals(deploymentVariables["JWT_SECRET"], properties.getRequiredProperty("glifo.jwt.secret"))
        assertEquals(deploymentVariables["CORS_ALLOWED_ORIGIN"], properties.getRequiredProperty("glifo.cors.allowed-origins"))
    }

    @Test
    fun strictPropertyResolutionRejectsMissingVariables() {
        val requiredProperties = mapOf(
            "DB_JDBC_URL" to "spring.datasource.url",
            "DB_USERNAME" to "spring.datasource.username",
            "DB_PASSWORD" to "spring.datasource.password",
            "JWT_SECRET" to "glifo.jwt.secret",
            "CORS_ALLOWED_ORIGIN" to "glifo.cors.allowed-origins"
        )
        requiredProperties.forEach { (variable, property) ->
            val properties = resolver(deploymentVariables - variable)
            assertFailsWith<IllegalArgumentException>(variable) {
                properties.getRequiredProperty(property)
            }
        }
    }

    @Test
    fun portBindsAllInterfacesAndSupportsProviderOverride() {
        assertEquals("0.0.0.0", resolver().getRequiredProperty("server.address"))
        assertEquals("8080", resolver().getRequiredProperty("server.port"))
        assertEquals("10000", resolver(mapOf("PORT" to "10000")).getRequiredProperty("server.port"))
    }

    @Test
    fun standaloneCloudRoutesMatchExistingApiRoutes() {
        val cloud = resolver(includeDevelopment = false)
        val sources = MutablePropertySources()
        sources.addLast(ResourcePropertySource("classpath:application.properties"))
        val development = PropertySourcesPropertyResolver(sources)
        listOf("url.api.v1", "url.health", "url.auth", "url.courses", "url.enrollments", "url.notes", "url.devices", "url.sync", "url.ai-usage")
            .forEach { property ->
                assertEquals(development.getRequiredProperty(property), cloud.getRequiredProperty(property), property)
            }
    }

    @Test
    fun cloudLimitsHaveDefaultsAndCanBeConfigured() {
        assertEquals("5", resolver().getRequiredProperty("spring.datasource.hikari.maximum-pool-size"))
        assertEquals("86400000", resolver().getRequiredProperty("glifo.jwt.expiration-ms"))
        val properties = resolver(mapOf("DB_MAX_POOL_SIZE" to "3", "JWT_EXPIRATION_MS" to "60000"))
        assertEquals("3", properties.getRequiredProperty("spring.datasource.hikari.maximum-pool-size"))
        assertEquals("60000", properties.getRequiredProperty("glifo.jwt.expiration-ms"))
    }

    @Test
    fun datasourceBinderUsesDeploymentCredentials() {
        val bound = bindDatasource(deploymentVariables)
        assertEquals(deploymentVariables["DB_JDBC_URL"], bound["url"])
        assertEquals(deploymentVariables["DB_USERNAME"], bound["username"])
        assertEquals(deploymentVariables["DB_PASSWORD"], bound["password"])
        assertEquals("org.postgresql.Driver", bound["driver-class-name"])
    }

    @Test
    fun datasourceBinderPreservesUnresolvedMarkersInsteadOfDevelopmentCredentials() {
        mapOf("DB_JDBC_URL" to "url", "DB_USERNAME" to "username", "DB_PASSWORD" to "password")
            .forEach { (variable, property) ->
                assertEquals("$" + "{" + variable + "}", bindDatasource(deploymentVariables - variable)[property])
            }
    }

    @Test
    fun cloudKeepsCharsetAndLeavesAiUnconfiguredUnlessExplicitlyEnabled() {
        val defaults = resolver(includeDevelopment = false)
        assertEquals("UTF-8", defaults.getRequiredProperty("spring.jpa.properties.hibernate.hbm2ddl.charset_name"))
        assertEquals("", defaults.getRequiredProperty("openai.api-key"))
        assertEquals("gpt-4o-mini", defaults.getRequiredProperty("openai.model"))
        val configured = resolver(mapOf("OPENAI_API_KEY" to "fixture-only-api-key", "OPENAI_MODEL" to "fixture-model"))
        assertEquals("fixture-only-api-key", configured.getRequiredProperty("openai.api-key"))
        assertEquals("fixture-model", configured.getRequiredProperty("openai.model"))
    }

    @Test
    fun allBaseKeysArePreservedOrIntentionallyExcluded() {
        val developmentKeys = ResourcePropertySource("classpath:application.properties").propertyNames.toSet()
        val cloudKeys = ResourcePropertySource("classpath:application-cloud.properties").propertyNames.toSet()
        val intentionallyExcluded = setOf("spring.profiles.active", "spring.config.import")
        assertEquals(emptySet(), developmentKeys - cloudKeys - intentionallyExcluded)
    }

    @Test
    fun cloudDisablesFcmAndCredentialDiscoveryByDefault() {
        val defaults = resolver(includeDevelopment = false)
        assertEquals("false", defaults.getRequiredProperty("glifo.fcm.enabled"))
        assertEquals("false", defaults.getRequiredProperty("glifo.fcm.use-application-default"))
        assertEquals("", defaults.getRequiredProperty("glifo.fcm.credentials-path"))
        assertEquals("", defaults.getRequiredProperty("glifo.fcm.credentials-file"))
        val client = FirebaseAdminFcmClient(FcmProperties(
            enabled = defaults.getRequiredProperty("glifo.fcm.enabled").toBooleanStrict(),
            credentialsPath = "missing-fixture-credentials.json"
        ))
        val outcome = client.send(FcmTargetedMessage("fixture-token", "fixture-event", emptyMap()))
        val failure = outcome as FcmSendOutcome.Failed
        assertEquals(FcmErrorKind.NOT_CONFIGURED, failure.errorKind)
        assertEquals("FCM_DISABLED", failure.errorCode)
    }

    @Test
    fun cloudFcmRequiresExplicitConfiguration() {
        val configured = resolver(mapOf(
            "GLIFO_FCM_ENABLED" to "true",
            "GLIFO_FCM_PROJECT_ID" to "fixture-project",
            "GLIFO_FCM_CREDENTIALS_PATH" to "/run/secrets/fixture.json",
            "GLIFO_FCM_CREDENTIALS_FILE" to "/run/secrets/alias.json",
            "GLIFO_FCM_MAX_ATTEMPTS" to "2",
            "GLIFO_FCM_RETRY_BACKOFF_MS" to "100"
        ), includeDevelopment = false)
        assertEquals("true", configured.getRequiredProperty("glifo.fcm.enabled"))
        assertEquals("fixture-project", configured.getRequiredProperty("glifo.fcm.project-id"))
        assertEquals("/run/secrets/fixture.json", configured.getRequiredProperty("glifo.fcm.credentials-path"))
        assertEquals("/run/secrets/alias.json", configured.getRequiredProperty("glifo.fcm.credentials-file"))
        assertEquals("false", configured.getRequiredProperty("glifo.fcm.use-application-default"))
        assertEquals("2", configured.getRequiredProperty("glifo.fcm.max-attempts"))
        assertEquals("100", configured.getRequiredProperty("glifo.fcm.retry-backoff-ms"))
    }
}
