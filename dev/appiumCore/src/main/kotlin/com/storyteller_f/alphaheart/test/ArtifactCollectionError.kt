/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.test

import com.storyteller_f.result.FailureDetails

/** Diagnostic collection failures must not be confused with the test's business failure. */
sealed interface ArtifactCollectionError : FailureDetails {
    data class CollectionFailed(override val cause: Throwable) : ArtifactCollectionError
}
