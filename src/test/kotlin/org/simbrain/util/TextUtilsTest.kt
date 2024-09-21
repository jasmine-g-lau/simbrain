package org.simbrain.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import smile.math.matrix.Matrix

class TextUtilsTest {

    val simpleText = "This is a simple sentence. This is not hard."

    /**
     * "Cat" and "dog" are in similar contexts.  "Please" and "dog" are not in similar contexts.
     */
    val similarText = "The cat can run. The dog can run. The cat eats food. The dog eats food. Please bring lunch to the table."

    val harderText = "In spite of these three obstacles, the fragmentary Don Quixote of Menard is more subtle than that of Cervantes. The latter indulges in a rather coarse opposition between tales of knighthood and the meager, provincial reality of his country; Menard chooses as 'reality' the land of Carmen during the century of Lepanto and Lope. What Hispanophile would not have advised Maurice Barres or Dr. Rodrigues Larreta to make such a choice! Menard, as if it were the most natural thing in the world, eludes them."

    val windowSizeText = "Albert ran into the store, while Jean walked into the store. Jean packed all the books, after Albert read all the books."

    val mlkText = "And so even though we face the difficulties of today and tomorrow, I still have a dream. It is a dream deeply rooted in the American dream. I have a dream that one day this nation will rise up and live out the true meaning of its creed: We hold these truths to be self-evident, that all men are created equal. I have a dream that one day on the red hills of Georgia, the sons of former slaves and the sons of former slave owners will be able to sit down together at the table of brotherhood. I have a dream that one day even the state of Mississippi, a state sweltering with the heat of injustice, sweltering with the heat of oppression, will be transformed into an oasis of freedom and justice. I have a dream that my four little children will one day live in a nation where they will not be judged by the color of their skin but by the content of their character. I have a dream today! I have a dream that one day, down in Alabama, with its vicious racists, with its governor having his lips dripping with the words of interposition and nullification, one day right there in Alabama little black boys and black girls will be able to join hands with little white boys and white girls as sisters and brothers. I have a dream today! I have a dream that one day every valley shall be exalted, and every hill and mountain shall be made low, the rough places will be made plain, and the crooked places will be made straight; and the glory of the Lord shall be revealed and all flesh shall see it together. This is our hope, and this is the faith that I go back to the South with. With this faith, we will be able to hew out of the mountain of despair a stone of hope. With this faith, we will be able to transform the jangling discords of our nation into a beautiful symphony of brotherhood. With this faith, we will be able to work together, to pray together, to struggle together, to go to jail together, to stand up for freedom together, knowing that we will be free one day. And this will be the day, this will be the day when all of God s children will be able to sing with new meaning: My country  tis of thee, sweet land of liberty, of thee I sing. Land where my fathers died, land of the Pilgrim s pride, From every mountainside, let freedom ring! And if America is to be a great nation, this must become true. And so let freedom ring from the prodigious hilltops of New Hampshire. Let freedom ring from the mighty mountains of New York. Let freedom ring from the heightening Alleghenies of Pennsylvania. Let freedom ring from the snow-capped Rockies of Colorado. Let freedom ring from the curvaceous slopes of California. But not only that: Let freedom ring from Stone Mountain of Georgia. Let freedom ring from Lookout Mountain of Tennessee. Let freedom ring from every hill and molehill of Mississippi. From every mountainside, let freedom ring. And when this happens, when we allow freedom ring, when we let it ring from every village and every hamlet, from every state and every city, we will be able to speed up that day when all of God s children, black men and white men, Jews and Gentiles, Protestants and Catholics, will be able to join hands and sing in the words of the old Negro spiritual: Free at last! Free at last! Thank God Almighty, we are free at last!"
    @Test
    fun `test sentence parsing`() {
        val sentences =  simpleText.tokenizeSentencesFromDoc()
        assertEquals(2, sentences.size)
        assertEquals(4, harderText.tokenizeSentencesFromDoc().size)
    }

    @Test
    fun `punctuation is removed correctly`() {
        var punctRemoved = "(A,B)#C:::{A_B}[D]"
        punctRemoved = punctRemoved.removePunctuation()
        assertEquals("ABCABD", punctRemoved)
    }

    @Test
    fun `correct number of words parsed from sentence`() {
        val sample = "This, is text!"
        assertEquals(3, sample.tokenizeWordsFromString().size)
    }

    @Test
    fun `test lowercasing`() {
        val firstCapital = "Abc"
        val middleCapital = "aBc"
        assertEquals("abc", firstCapital.lowercase())
        assertEquals("abc", middleCapital.lowercase())
    }

    @Test
    fun `tabs and newlines removed by removeSpecialCharacters`() {
        val testString = "A\t\tb\n\nc"
        assertEquals(false,testString.removeSpecialCharacters().contains("[\n\r\t]"))
        assertEquals(5,testString.removeSpecialCharacters().length)
    }

    @Test
    fun `get unique tokens from sentences`() {
        val sentence = "a A a b. B c b d c c"
        val tokenizedSentence = sentence.tokenizeWordsFromString()
        val uniqueTokens = tokenizedSentence.uniqueTokensFromArray()
        // println(uniqueTokens)
        assertEquals(listOf("a","b","c","d"), uniqueTokens)
    }

    @Test
    fun `PPMI computed correctly`() {
        val A = arrayOf(
            doubleArrayOf(0.0, 3.0, 2.0),
            doubleArrayOf(1.0, 4.0, 0.0),
            doubleArrayOf(1.0, 0.0, 0.0)
        )
        val temporaryMatrix = Matrix.of(A)
        val adjustedMatrix = manualPPMI(temporaryMatrix, true)
        assertTrue(temporaryMatrix[0,1] > adjustedMatrix[0,0])
        assertEquals(temporaryMatrix[0,0], adjustedMatrix[0,0])
    }

    @Test
    fun `Remove stopwords`() {
        val tokens = simpleText.tokenizeWordsFromString().uniqueTokensFromArray()
        assertEquals(listOf<String>("simple","sentence","hard"), removeStopWords(tokens))
    }

    @Test
    fun `co-occurrence matrix is correct size`() {
        val tokens = simpleText.tokenizeWordsFromString().uniqueTokensFromArray()
        val cooccurrenceMatrix = generateCooccurrenceMatrix(simpleText, 2, true, removeStopwords = false)
        assertEquals(tokens.size, cooccurrenceMatrix.tokenVectorMatrix.nrow())
        assertEquals(tokens.size, cooccurrenceMatrix.tokenVectorMatrix.ncol())
    }

    @Test
    fun `word embedding have correct size`() {
        val tokenizedSentence = harderText.tokenizeWordsFromString()
        val tokens = tokenizedSentence.uniqueTokensFromArray()
        val cooccurrenceMatrix = generateCooccurrenceMatrix(harderText, 2, true)
        assertEquals(tokens.size, cooccurrenceMatrix.get("obstacles").size)
        assertEquals(tokens.size, cooccurrenceMatrix.get("Quixote").size) // issue wascapital Q
    }

    @Test
    fun `co-occurence matrix window size correctly affects similarity`() {
        val cooccurrenceMatrixShort = generateCooccurrenceMatrix(windowSizeText, 1, true)
        val cooccurrenceMatrixLong = generateCooccurrenceMatrix(windowSizeText, 4, true)
        val smallWindowSimilarity = embeddingSimilarity(
            cooccurrenceMatrixShort.get("Jean"),
            cooccurrenceMatrixShort.get("Albert"))
        val longWindowSimilarity = embeddingSimilarity(
            cooccurrenceMatrixLong.get("Jean"),
            cooccurrenceMatrixLong.get("Albert"))
        assertTrue(smallWindowSimilarity < longWindowSimilarity)
    }

    @Test
    fun `computes cosine similarity between two vectors`() {
        val tokenizedSentence = similarText.tokenizeWordsFromString()
        val tokens = tokenizedSentence.uniqueTokensFromArray()
        val cooccurrenceMatrix = generateCooccurrenceMatrix(similarText, 2, true)
        val vectorA = cooccurrenceMatrix.get("cat")
        val vectorB = cooccurrenceMatrix.get("dog")
        val vectorC = cooccurrenceMatrix.get("table")
        assertTrue(embeddingSimilarity(vectorA, vectorB) > embeddingSimilarity(vectorB, vectorC) )
    }

    @Test
    fun `no NaN values in co-occurrence matrix`() {
        val cocMatrix = generateCooccurrenceMatrix(mlkText, 2, true)
        for (index in 0..cocMatrix.tokens.size){
            // println(coocMatrix[index,0].toString())
            // println(coocMatrix.row(index).toString())
            if (cocMatrix.tokenVectorMatrix[index,0].isNaN()) println(index) // Only checking first value
        // since NaNs occur
        // as the whole row/column
        }
    }

    @Test
    fun `remove stopwords removes words it should`() {
        // "This", "not", and "is" are stop words, the names are not
        val coc = generateCooccurrenceMatrix("This is Balthazar not Mordrax", 2, true)
        val filteredTokens = removeStopWords(coc.tokens)
        assertEquals(2, filteredTokens.size)
    }

    @Test
    fun `removeWords removes words from strings and words only`() {
        val inputString = "worldblahtest world blah test"
        val words = listOf("world", "test")
        assertEquals("worldblahtest blah", inputString.removeWords(words))
    }

    @Test
    fun `convertCamelCaseToSpaces should work on UpperCamelCase`() {
        val inputString = "ThisIsCamelCase"
        assertEquals("This Is Camel Case", inputString.convertCamelCaseToSpaces())
    }

    @Test
    fun `convertCamelCaseToSpaces should work on lowerCamelCase`() {
        val inputString = "thisIsCamelCase"
        assertEquals("This Is Camel Case", inputString.convertCamelCaseToSpaces())
    }

    @Test
    fun `convertCamelCaseToSpaces should not break consecutive upper case letters`() {
        val inputString = "ThisIsCamelCaseABC"
        assertEquals("This Is Camel Case ABC", inputString.convertCamelCaseToSpaces())
    }


    @Test
    fun `build corpus list`() {
        val context = "The quick brown fox jumps over the lazy dog".split(" ")
        val corpus = generateAutoregressivePairs(context)
        val expected = listOf(
            "The" to "quick",
            "The quick" to "brown",
            "The quick brown" to "fox",
            "The quick brown fox" to "jumps",
            "The quick brown fox jumps" to "over",
            "The quick brown fox jumps over" to "the",
            "The quick brown fox jumps over the" to "lazy",
            "The quick brown fox jumps over the lazy" to "dog"
        ).map { (a, b) -> a.split(" ") to b }
        assert(corpus.size == expected.size)
        (corpus zip expected).forEach { (c, e) ->
            val (expectedContext, expectedTarget) = e
            val (corpusContext, corpusTarget) = c
            assertArrayEquals(expectedContext.toTypedArray(), corpusContext.toTypedArray())
            assertEquals(expectedTarget, corpusTarget)
        }
    }

}

