package example.graphql.scalars

import graphql.language.IntValue
import graphql.language.StringValue
import graphql.language.Value
import graphql.schema.Coercing
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException
import graphql.schema.GraphQLScalarType
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

val graphQLLong = GraphQLScalarType.newScalar()
    .name("Long").description("A 64-bit signed integer")
    .coercing(LongCoercing).build()


object LongCoercing : Coercing<Long?, Long?> {
    private fun convertImpl(input: Any): Long? {
        return if (input is Long) {
            input
        } else if (isNumberIsh(input)) {
            val value: BigDecimal = try {
                BigDecimal(input.toString())
            } catch (e: NumberFormatException) {
                return null
            }
            try {
                value.longValueExact()
            } catch (e: ArithmeticException) {
                null
            }
        } else {
            null
        }
    }

    override fun serialize(input: Any): Long {
        return convertImpl(input) ?: throw CoercingSerializeException(
            "Expected type 'Long' but was '${input.javaClass.simpleName}'."
        )
    }

    override fun parseValue(input: Any): Long {
        return convertImpl(input) ?: throw CoercingParseValueException(
            "Expected type 'Long' but was '${input.javaClass.simpleName}'."
        )
    }

    override fun parseLiteral(input: Any): Long {
        if (input is StringValue) {
            return try {
                input.value.toLong()
            } catch (e: NumberFormatException) {
                throw CoercingParseLiteralException(
                    "Expected value to be a Long but it was '$input'"
                )
            }
        } else if (input is IntValue) {
            val value = input.value
            if (value < Long.MIN_VALUE.toBigInteger() || value > Long.MAX_VALUE.toBigInteger()) {
                throw CoercingParseLiteralException(
                    "Expected value to be in the Long range but it was '$value'"
                )
            }
            return value.toLong()
        }
        throw CoercingParseLiteralException(
            "Expected AST type 'IntValue' or 'StringValue' but was '${input.javaClass.simpleName}'."
        )
    }

    override fun valueToLiteral(input: Any): Value<out Value<*>> {
        val result = Objects.requireNonNull(convertImpl(input))
        return IntValue.newIntValue(BigInteger.valueOf(result!!)).build()
    }

    private fun isNumberIsh(input: Any): Boolean {
        return input is Number || input is String
    }
}
