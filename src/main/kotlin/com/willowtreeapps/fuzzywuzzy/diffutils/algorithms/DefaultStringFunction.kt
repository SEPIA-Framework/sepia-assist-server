package com.willowtreeapps.fuzzywuzzy.diffutils.algorithms

import com.willowtreeapps.fuzzywuzzy.ToStringFunction

val pattern: String = "(?U)[^\\p{Alnum}]"		//Embedded flag for UNICODE_CHARACTER_CLASS

class DefaultStringFunction : ToStringFunction<String> {
	
	/**
     * Performs the default string processing on the item string
     *
     * @param `item` Input string
     * @return The processed string
     */
    override fun apply(item: String) = subNonAlphaNumeric(item, " ").lowercase().trim { it <= ' ' }
	
	companion object {
		
		//private const val nonUnicodePattern = "[^\\p{Alnum}]"
        private val r = Regex(pattern)
		
		/**
         * Substitute non alphanumeric characters.
         *
         * @param in The input string
         * @param sub The string to substitute with
         * @return The replaced string
         */
        fun subNonAlphaNumeric(`in`: String, sub: String): String {
            val m = r.find(`in`)
            return if (m != null) {
                r.replace(`in`, sub)
            } else {
                `in`
            }
        }
		
	}
}