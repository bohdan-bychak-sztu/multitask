package com.bbm.multitask.data.model.lPGraphicalMethod

class LinearProgrammingProblem {
    var objectiveFunction: ObjectiveFunction? = null
    val constraints: MutableList<Constraint> = mutableListOf()

    fun addConstraint(constraint: Constraint) {
        constraints.add(constraint)
    }

    fun clearConstraints() {
        constraints.clear()
    }

    fun clear(){
        objectiveFunction = null
        constraints.clear()
    }

    fun deleteConstraint(constraint: Constraint) {
        constraints.remove(constraint)
    }
}

class ObjectiveFunction {
    val objectiveFunction: Pair<Double, Double> = 0.0 to 0.0
    val isMax: Boolean = true
}
