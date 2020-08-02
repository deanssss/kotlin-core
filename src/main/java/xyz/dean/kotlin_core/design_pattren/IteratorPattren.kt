package xyz.dean.kotlin_core.design_pattren

data class Book(val name: String)

class Bookcase(val books: List<Book>)
operator fun Bookcase.iterator(): Iterator<Book> = books.iterator()