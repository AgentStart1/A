/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.utils

fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)

fun lcm(a: Int, b: Int): Int = a * b / gcd(a, b)
