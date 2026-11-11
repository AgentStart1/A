#!/usr/bin/env bash
# Parse a fixed option-to-variable map without evaluating argument contents.
parse_named_options() {
    local option_usage=$1 option_map=$2
    shift 2
    local option_entry option_variable
    while [[ $# -gt 0 ]]; do
        if [[ $1 == --help ]]; then
            printf '%s\n' "$option_usage"
            exit 0
        fi
        option_variable=
        for option_entry in $option_map; do
            if [[ $1 == "--${option_entry%%:*}" ]]; then
                option_variable=${option_entry#*:}
                break
            fi
        done
        if [[ -z $option_variable ]]; then
            printf 'Unknown argument: %s\n%s\n' "$1" "$option_usage" >&2
            return 1
        fi
        if [[ $# -lt 2 || $2 == --* || -z $2 ]]; then
            printf '%s requires a value.\n' "$1" >&2
            return 1
        fi
        printf -v "$option_variable" '%s' "$2"
        shift 2
    done
}

require_named_options() {
    local required_option
    for required_option in "$@"; do
        if [[ -z ${!required_option} ]]; then
            printf 'Missing required option: %s\n' "$required_option" >&2
            return 1
        fi
    done
}
