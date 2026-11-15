package com.maisonglow.ai;

import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.service.CustomerService;
import java.awt.Color;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Offline visual preview with a color tint; it does not predict a real treatment result. */
public final class SimulatedTryOnService implements TryOnService {
    /** Warning shared by the image and result. */
    public static final String DISCLAIMER = TryOnImages.DISCLAIMER;
    private static final Map<String, Color> COLORS = Map.of(
        "rojo cobrizo", new Color(172, 69, 39),
        "rubio dorado", new Color(207, 160, 62),
        "castaño oscuro", new Color(80, 48, 35));
    private final TryOnImages images;

    /** Creates an offline service that checks customers through the business service. */
    public SimulatedTryOnService(CustomerService customers) {
        images = new TryOnImages(customers);
    }

    /** Returns the transformation names to offer in the JavaFX selector. */
    public static List<String> supportedTransformations() {
        return TryOnImages.supportedTransformations();
    }

    @Override
    public TryOnResult generate(TryOnRequest request) throws ValidationException, DataAccessException {
        var source = images.validate(request);
        String transformation = request.transformation().trim().toLowerCase(Locale.ROOT);
        return TryOnImages.save(source, transformation, COLORS.get(transformation));
    }
}
