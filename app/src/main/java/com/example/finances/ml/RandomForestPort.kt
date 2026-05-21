package com.example.finances

import android.content.Context
import org.json.JSONObject

class RandomForestPort(context: Context) {

    private data class Tree(
        val features: IntArray,
        val thresholds: DoubleArray,
        val childrenLeft: IntArray,
        val childrenRight: IntArray,
        val values: Array<DoubleArray>
    )

    private val trees = mutableListOf<Tree>()
    private val scalerMean = DoubleArray(8)
    private val scalerStd = DoubleArray(8)
    private val nEstimators: Int

    init {
        val json = context.assets.open("forest_model.json").bufferedReader().readText()
        val root = JSONObject(json)

        nEstimators = root.getInt("n_estimators")

        val meanArr = root.getJSONArray("scaler_mean")
        val stdArr = root.getJSONArray("scaler_std")
        for (i in 0 until 8) {
            scalerMean[i] = meanArr.getDouble(i)
            scalerStd[i] = stdArr.getDouble(i)
        }

        val treesArr = root.getJSONArray("trees")
        for (t in 0 until treesArr.length()) {
            val treeJson = treesArr.getJSONObject(t)
            val features = treeJson.getJSONArray("features")
            val thresholds = treeJson.getJSONArray("thresholds")
            val left = treeJson.getJSONArray("children_left")
            val right = treeJson.getJSONArray("children_right")
            val valuesArr = treeJson.getJSONArray("values")

            val n = features.length()
            val f = IntArray(n); val th = DoubleArray(n)
            val l = IntArray(n); val r = IntArray(n)
            val v = Array(n) { DoubleArray(2) }

            for (i in 0 until n) {
                f[i] = features.getInt(i)
                th[i] = thresholds.getDouble(i)
                l[i] = left.getInt(i)
                r[i] = right.getInt(i)
                val vals = valuesArr.getJSONArray(i)
                v[i][0] = vals.getDouble(0)
                v[i][1] = vals.getDouble(1)
            }
            trees.add(Tree(f, th, l, r, v))
        }
    }

    fun predictProbability(features: FloatArray): Float {
        val scaled = FloatArray(8)
        for (i in 0 until 8) {
            scaled[i] = ((features[i].toDouble() - scalerMean[i]) / scalerStd[i]).toFloat()
        }

        var totalProb = 0f
        for (tree in trees) {
            totalProb += traverseTree(tree, scaled)
        }
        return totalProb / nEstimators
    }

    private fun traverseTree(tree: Tree, features: FloatArray): Float {
        var node = 0
        while (tree.childrenLeft[node] != -1) {
            val feature = tree.features[node]
            val threshold = tree.thresholds[node]
            node = if (features[feature] <= threshold) {
                tree.childrenLeft[node]
            } else {
                tree.childrenRight[node]
            }
        }
        val values = tree.values[node]
        return (values[1] / (values[0] + values[1])).toFloat()
    }
}