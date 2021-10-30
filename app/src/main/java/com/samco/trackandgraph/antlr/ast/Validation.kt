//   Copyright 2021 Federico Tomassetti
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.

//  Modified


package com.samco.trackandgraph.antlr.ast

import com.samco.trackandgraph.R
import com.samco.trackandgraph.antlr.evaluation.DoubleDeclarationError
import com.samco.trackandgraph.antlr.evaluation.NotDeclaredError
import com.samco.trackandgraph.antlr.evaluation.ReferencedBeforeDeclarationError
import com.samco.trackandgraph.antlr.evaluation.UnexistingVariableError
import java.util.*
import kotlin.reflect.KFunction2

open class DatatransformationFunctionError(fallbackMessage: String, var position: Position,
    val messageFunction: (KFunction2<Int, Array<out Any?>, String>) -> String) : Exception(fallbackMessage) {

    constructor(
        fallbackMessage: String,
        position: Position,
        localizedMessageId: Int,
        vararg localizedMessageArgs: String
    ) : this(fallbackMessage, position,
        { getString -> getString(localizedMessageId, localizedMessageArgs) }
    )

    fun getPos(): Position = position
    fun getFallbackMessage(): String = super.message.toString()
    fun getLocalizedMessage(getString: KFunction2<Int, Array<out Any?>, String>): String =
        messageFunction(getString)

    private fun makeMessageFull(message: String): String =
        "${this.javaClass.simpleName} at ${this.getPos().start}: $message"

    private fun makeMessageFullLocalized(
        message: String,
        getString: KFunction2<Int, Array<out Any?>, String>
    ): String = getString(
        R.string.errormsg_error_at_position,
        arrayOf(this.javaClass.simpleName, this.getPos().start.toString(), message)
    )

    fun fullFallbackMessage(): String = makeMessageFull(this.getFallbackMessage())
    fun fullLocalizedMessage(getString: KFunction2<Int, Array<out Any?>, String>): String =
        makeMessageFullLocalized(this.getLocalizedMessage(getString), getString)

}

data class ListOfErrors(val errors: List<DatatransformationFunctionError>) :
    DatatransformationFunctionError(fallbackMessage = errors.joinToString(separator = "\n") { it.fullFallbackMessage() },
        position = errors.first().getPos(),
        { getString -> errors.joinToString(separator = "\n") { it.fullLocalizedMessage(getString) } }
    )

fun DatatransformationFunction.validate(externalInputNames: Set<String> = emptySet()) : List<DatatransformationFunctionError> {
    val errors = LinkedList<DatatransformationFunctionError>()

    // check a variable is not duplicated
    val varsByName = HashMap<String, VarDeclaration>()
    this.specificProcess(VarDeclaration::class.java) {
        if (varsByName.containsKey(it.varName)) {
            errors.add(DoubleDeclarationError(it.varName, it.position!!))
        } else {
            varsByName[it.varName] = it
        }
    }

    // check a variable is not referred before being declared
    this.specificProcess(VarReference::class.java) {
        if (it.varName !in externalInputNames) {
            if (!varsByName.containsKey(it.varName) && !externalInputNames.contains(it.varName)) {
                errors.add(UnexistingVariableError(it.varName, it.position!!))
            } else if (it.isBefore(varsByName[it.varName]!!)) {
                errors.add(ReferencedBeforeDeclarationError(it.varName, it.position!!))
            }
        }

    }
    this.specificProcess(Assignment::class.java) {
        if (!varsByName.containsKey(it.varName)) {
            errors.add(NotDeclaredError(it.varName, it.position!!))
        } else if (it.isBefore(varsByName[it.varName]!!)) {
            errors.add(ReferencedBeforeDeclarationError(it.varName, it.position!!))
        }
    }

    return errors
}
