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

import java.util.*

interface ErrorInterface{
    fun getPos() : Position
    fun getMes(): String
}

data class Error(val message: String, val position: Position) : ErrorInterface {
    override fun getPos(): Position = position
    override fun getMes(): String = message
}

open class ThrowableError(message: String, var position: Position) : ErrorInterface, Exception(message) {
    override fun getPos(): Position = position
    override fun getMes(): String = super.message.toString()
}

fun DatatransformationFunction.validate() : List<Error> {
    val errors = LinkedList<Error>()

    // check a variable is not duplicated
    val varsByName = HashMap<String, VarDeclaration>()
    this.specificProcess(VarDeclaration::class.java) {
        if (varsByName.containsKey(it.varName)) {
            errors.add(Error("A variable named '${it.varName}' has been already declared at ${varsByName[it.varName]!!.position!!.start}",
                    it.position!!))
        } else {
            varsByName[it.varName] = it
        }
    }

    // check a variable is not referred before being declared
    this.specificProcess(VarReference::class.java) {
        if (!varsByName.containsKey(it.varName)) {
            errors.add(Error("There is no variable named '${it.varName}'", it.position!!))
        } else if (it.isBefore(varsByName[it.varName]!!)) {
            errors.add(Error("You cannot refer to variable '${it.varName}' before its declaration", it.position!!))
        }
    }
    this.specificProcess(Assignment::class.java) {
        if (!varsByName.containsKey(it.varName)) {
            errors.add(Error("There is no variable named '${it.varName}'", it.position!!))
        } else if (it.isBefore(varsByName[it.varName]!!)) {
            errors.add(Error("You cannot refer to variable '${it.varName}' before its declaration", it.position!!))
        }
    }

    return errors
}
