package com.ssj.statuswindow.core.ml
enum class Category{FOOD,ETC}
interface CategoryInferencer{fun infer(t:String):Category}