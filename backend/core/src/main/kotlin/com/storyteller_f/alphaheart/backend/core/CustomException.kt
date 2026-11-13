/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core

class UnauthorizedException : Exception()

class ForbiddenException(message: String = "Invalid operation") : Exception(message)

class CustomBadRequestException(message: String) : Exception(message)
