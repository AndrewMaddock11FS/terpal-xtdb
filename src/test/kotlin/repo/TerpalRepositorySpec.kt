package repo

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.exoquery.sql.JsonValue
import io.exoquery.sql.SqlJson
import io.exoquery.sql.jdbc.JdbcEncoderAny
import io.exoquery.sql.jdbc.TerpalContext
import io.kotest.core.extensions.install
import io.kotest.core.spec.style.FreeSpec
import io.kotest.extensions.testcontainers.JdbcDatabaseContainerExtension
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import xtdb.api.Xtdb
import xtdb.api.pgwireServer
import java.sql.Types

class TerpalRepositorySpec :
    FreeSpec({
        "testcontainers postgres" - {
            "findById person - use JdbcEncoder setObject with target SQL type NONE" {
                val postgresImageName = DockerImageName.parse("postgres:latest")
                val postgresContainer: PostgreSQLContainer<*> = PostgreSQLContainer(postgresImageName)
                    .withInitScript("people.sql")
                val dataSource = install(JdbcDatabaseContainerExtension(postgresContainer))
                val ctx = TerpalContext.Postgres(dataSource)

//                val jdbcEncoderSqlJson = JdbcEncoderAny.fromFunction(Types.OTHER) { ctx, v: SqlJson, i ->
//                    ctx.stmt.setObject(i, v.value, Types.OTHER)
//                }
//                val ctx = object : TerpalContext.Postgres(dataSource) {
//                    override val additionalEncoders =
//                        super.additionalEncoders.filter { it.type != SqlJson::class }
//                            .toSet() + jdbcEncoderSqlJson
//                }

                val repository = terpalRepository(ctx)

                runBlocking {
                    repository.save(PERSON)
                    repository.findById(1) shouldBe listOf(PERSON)
                }
            }
        }

        "in-memory xtdb" - {
            "findById person - use JdbcEncoder setObject with target SQL type NONE" {
                Xtdb.openNode { pgwireServer { port = 0 } }
                    .run {
                        val hikariConfig = HikariConfig().apply {
                            jdbcUrl = "jdbc:postgresql://localhost:$pgPort/xtdb"
                            driverClassName = "org.postgresql.Driver"
                        }
                        val dataSource = HikariDataSource(hikariConfig)
                        val ctx = TerpalContext.Postgres(dataSource)

                        val repository = terpalRepository(ctx)

                        runBlocking {
                            repository.save(PERSON)

                            // the saved person only has an id and has not persisted the names column
                            println("select people: ${openQuery("select * from people").findFirst()}")

                            // here we get a null pointer - getString(...) must not be null
                            // this is because the names column was not saved
                            repository.findById(1) shouldBe listOf(PERSON)
                        }
                    }
            }

            "findById person - override JdbcEncoder setObject with no target SQL type" {
                Xtdb.openNode { pgwireServer { port = 0 } }
                    .run {
                        val hikariConfig = HikariConfig().apply {
                            jdbcUrl = "jdbc:postgresql://localhost:$pgPort/xtdb"
                            driverClassName = "org.postgresql.Driver"
                        }
                        val dataSource = HikariDataSource(hikariConfig)
                        val jdbcEncoderSqlJson = JdbcEncoderAny.fromFunction(Types.OTHER) { ctx, v: SqlJson, i ->
                            ctx.stmt.setString(i, v.value)
                        }
                        val ctx = object : TerpalContext.Postgres(dataSource) {
                            override val additionalEncoders =
                                super.additionalEncoders.filter { it.type != SqlJson::class }
                                    .toSet() + jdbcEncoderSqlJson
                        }

                        val repository = terpalRepository(ctx)

                        runBlocking {
                            repository.save(PERSON)

                            // the saved person only has an id and XT has not persisted the names column
                            println("select people: ${openQuery("select * from people").findFirst()}")

                            // this is failing!!!
                            // here we get a null pointer - getString(...) must not be null
                            // this is because the names column was not saved
                            repository.findById(1) shouldBe listOf(PERSON)
                        }
                    }
            }

            "use prepared insert statement - setObject with no target SQL type" {
                Xtdb.openNode { pgwireServer { port = 0 } }
                    .run {
                        val hikariConfig = HikariConfig().apply {
                            jdbcUrl = "jdbc:postgresql://localhost:$pgPort/xtdb"
                            driverClassName = "org.postgresql.Driver"
                        }
                        val dataSource = HikariDataSource(hikariConfig)

                        // use prepared statement rather than repository insert
                        val prepareStatement = dataSource
                            .connection
                            .prepareStatement("INSERT INTO people (_id, names) VALUES (?, ?)")
                        prepareStatement.setInt(1, PERSON.id)
                        // NOTE: there is no SQL type for the setObject
                        prepareStatement.setObject(2, Json.encodeToString(PERSON.names.value))
                        prepareStatement.executeUpdate()

                        val ctx = TerpalContext.Postgres(dataSource)
                        val repository = terpalRepository(ctx)

                        runBlocking {
                            println("select people: ${openQuery("select * from people").findFirst()}")

                            repository.findById(1) shouldBe listOf(PERSON)
                        }
                    }
            }
        }
    })

private val NAMES = listOf(Name("John"), Name("Paul"), Name("George"), Name("Ringo"))
private val PERSON = Person(id = 1, names = JsonValue(NAMES))
