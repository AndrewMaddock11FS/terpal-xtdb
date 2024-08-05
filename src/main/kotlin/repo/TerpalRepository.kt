package repo

import io.exoquery.sql.JsonValue
import io.exoquery.sql.Param
import io.exoquery.sql.jdbc.JdbcContext
import io.exoquery.sql.jdbc.Sql
import io.exoquery.sql.jdbc.runOn
import kotlinx.serialization.Serializable

@Serializable
data class Name(val name: String)

@Serializable
data class Person(val id: Int, val names: JsonValue<List<Name>>)

interface Repository {
    suspend fun save(person: Person)
    suspend fun findById(id: Int): List<Person>
}

fun terpalRepository(ctx: JdbcContext) = object : Repository {
    override suspend fun save(person: Person) {
        Sql("INSERT INTO people (_id, names) VALUES (${person.id}, ${Param.withSer(person.names)})")
            .action()
            .runOn(ctx)
    }

    override suspend fun findById(id: Int): List<Person> {
        return Sql("SELECT _id, names FROM people")
            .queryOf<Person>()
            .runOn(ctx)
    }
}