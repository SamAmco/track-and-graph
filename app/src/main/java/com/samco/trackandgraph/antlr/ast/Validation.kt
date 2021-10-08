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

import com.samco.trackandgraph.antlr.evaluation.DoubleDeclarationError
import com.samco.trackandgraph.antlr.evaluation.NotDeclaredError
import com.samco.trackandgraph.antlr.evaluation.ReferencedBeforeDeclarationError
import com.samco.trackandgraph.antlr.evaluation.UnexistingVariableError
import java.util.*


open class Error(message: String, var position: Position) : Exception(message) {
    fun getPos(): Position = position
    fun getMes(): String = super.message.toString()

    fun fullMessage() : String = "${this.javaClass.simpleName} at ${this.getPos().start}: ${this.getMes()}"
}

data class ListOfErrors(val errors: List<Error>)
    : Exception(errors.joinToString(separator = "\n") { it.fullMessage() } )

fun DatatransformationFunction.validate(externalInputNames: Set<String> = emptySet()) : List<Error> {
    val errors = LinkedList<Error>()

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
