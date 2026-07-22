package com.hsf302.carshowroom.controller;

import com.hsf302.carshowroom.entity.Product;
import com.hsf302.carshowroom.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class ProductImageController {
    private final ProductRepository productRepository;

    @GetMapping(value = "/product-images/{sku}.svg", produces = "image/svg+xml")
    public ResponseEntity<String> productImage(@PathVariable String sku) {
        Product product = productRepository.findBySku(sku)
                .orElse(null);
        if (product == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("");
        }

        String productName = escape(product.getProductName());
        String categoryName = escape(product.getCategory() == null ? "Auto part" : product.getCategory().getCategoryName());
        String safeSku = escape(product.getSku());
        String primary = colorFor(product.getSku(), 0x1F2937);
        String accent = colorFor(product.getProductName(), 0xF59E0B);

        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="960" height="640" viewBox="0 0 960 640" role="img" aria-label="%s">
                  <defs>
                    <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
                      <stop offset="0" stop-color="%s"/>
                      <stop offset="1" stop-color="#111827"/>
                    </linearGradient>
                    <radialGradient id="shine" cx="35%%" cy="25%%" r="60%%">
                      <stop offset="0" stop-color="#ffffff" stop-opacity="0.32"/>
                      <stop offset="1" stop-color="#ffffff" stop-opacity="0"/>
                    </radialGradient>
                  </defs>
                  <rect width="960" height="640" rx="44" fill="url(#bg)"/>
                  <rect width="960" height="640" rx="44" fill="url(#shine)"/>
                  <g opacity="0.24" stroke="#ffffff" stroke-width="18" fill="none">
                    <circle cx="210" cy="448" r="94"/>
                    <circle cx="750" cy="448" r="94"/>
                    <path d="M292 448h344l60-136H414l-74 136"/>
                    <path d="M372 312l74-92h196l74 92"/>
                  </g>
                  <g transform="translate(120 112)">
                    <rect x="0" y="0" width="720" height="416" rx="34" fill="#ffffff" opacity="0.92"/>
                    <rect x="0" y="0" width="720" height="14" rx="7" fill="%s"/>
                    <text x="48" y="96" font-family="Arial, sans-serif" font-size="32" font-weight="700" fill="#111827">%s</text>
                    <text x="48" y="170" font-family="Arial, sans-serif" font-size="54" font-weight="800" fill="#0f172a">%s</text>
                    <text x="48" y="246" font-family="Arial, sans-serif" font-size="30" font-weight="700" fill="%s">%s</text>
                    <rect x="48" y="300" width="236" height="56" rx="28" fill="%s" opacity="0.14"/>
                    <text x="76" y="337" font-family="Arial, sans-serif" font-size="24" font-weight="700" fill="%s">%s</text>
                  </g>
                </svg>
                """.formatted(productName, primary, accent, categoryName, productName, accent, safeSku, accent, accent, safeSku);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("image/svg+xml"))
                .body(svg);
    }

    private String colorFor(String value, int salt) {
        int hash = (value == null ? "" : value).hashCode() ^ salt;
        int hue = Math.floorMod(hash, 360);
        return "hsl(" + hue + ", 68%, 38%)";
    }

    private String escape(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
