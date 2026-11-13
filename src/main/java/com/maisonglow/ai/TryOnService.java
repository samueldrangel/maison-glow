package com.maisonglow.ai;

import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;

/** Provider-independent virtual try-on contract. */
public interface TryOnService {
    /** Validates the request and returns an image explicitly marked as a simulation. */
    TryOnResult generate(TryOnRequest request) throws ValidationException, DataAccessException;
}
