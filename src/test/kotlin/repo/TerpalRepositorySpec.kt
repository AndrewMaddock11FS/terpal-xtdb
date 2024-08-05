package repo

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.exoquery.sql.JsonValue
import io.exoquery.sql.jdbc.TerpalContext
import io.kotest.core.extensions.install
import io.kotest.core.spec.style.FreeSpec
import io.kotest.extensions.testcontainers.JdbcDatabaseContainerExtension
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import xtdb.api.Xtdb
import xtdb.api.pgwireServer

class TerpalRepositorySpec : FreeSpec({
    "testcontainers postgres" - {
        "findById person" {
            val postgresImageName = DockerImageName.parse("postgres:latest")
            val postgresContainer: PostgreSQLContainer<*> = PostgreSQLContainer(postgresImageName)
                .withInitScript("people.sql")
            val dataSource = install(JdbcDatabaseContainerExtension(postgresContainer))
            val repository = terpalRepository(TerpalContext.Postgres(dataSource))

            runBlocking {
                repository.save(PERSON)
                repository.findById(1) shouldBe listOf(PERSON)
            }
        }
    }

    "in-memory xtdb" - {
        "findById person" {
            Xtdb.openNode { pgwireServer { port = 0 } }
                .run {
                    val dataSource = HikariDataSource(
                        HikariConfig().apply {
                            jdbcUrl = "jdbc:postgresql://localhost:${pgPort}/xtdb"
                            driverClassName = "org.postgresql.Driver"
                        })
                    val repository = terpalRepository(TerpalContext.Postgres(dataSource))

                    runBlocking {
                        repository.save(PERSON)
                        // the saved person only has an id and has not persisted the names column
                        println("select people: ${openQuery("select * from people").findFirst()}")

                        // here the null pointer - getString(...) must not be null
                        // this is because the names were not saved
                        repository.findById(1) shouldBe listOf(PERSON)
                    }
                }
        }
    }
})

private val NAMES = listOf(Name("John"), Name("Paul"), Name("George"), Name("Ringo"))
private val PERSON = Person(id = 1, names = JsonValue(NAMES))
