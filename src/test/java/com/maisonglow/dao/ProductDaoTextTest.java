package com.maisonglow.dao;

import com.maisonglow.exception.DataAccessException;
import com.maisonglow.model.Product;
import com.maisonglow.model.ProductCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ProductDaoText tests")
class ProductDaoTextTest {

    @TempDir
    Path folder;

    private CategoryDao categoryDao;
    private ProductCategory hair;

    @BeforeEach
    void setUp() throws DataAccessException {
        categoryDao = new CategoryDaoText(folder.resolve("categories.txt"));
        hair = categoryDao.create(new ProductCategory(0, "Cabello"));
    }

    @Test
    @DisplayName("products survive a reload with their category, stock and state")
    void createAndReload() throws DataAccessException {
        Path file = folder.resolve("products.txt");
        Product product = new ProductDaoText(file, categoryDao)
            .create(new Product(0, "Shampoo", hair, 28000.5, 10, 3));
        product.deactivate();
        new ProductDaoText(file, categoryDao).update(product);

        Product reloaded = new ProductDaoText(file, categoryDao).findById(product.getId()).orElseThrow();

        assertThat(reloaded.getCategory().getName()).isEqualTo("Cabello");
        assertThat(reloaded.getPrice()).isEqualTo(28000.5);
        assertThat(reloaded.getStock()).isEqualTo(10);
        assertThat(reloaded.getMinimumStock()).isEqualTo(3);
        assertThat(reloaded.isActive()).isFalse();
    }

    @Test
    @DisplayName("findByName ignores case")
    void findByName() throws DataAccessException {
        ProductDao dao = new ProductDaoText(folder.resolve("products.txt"), categoryDao);
        dao.create(new Product(0, "Shampoo", hair, 28000, 10, 3));

        assertThat(dao.findByName("SHAMPOO")).isPresent();
        assertThat(dao.findByName("Gel")).isEmpty();
    }

    @Test
    @DisplayName("a product pointing to a missing category is reported")
    void missingCategory() throws IOException {
        Path file = folder.resolve("products.txt");
        Files.write(file, List.of("1|Shampoo|99|28000.0|10|3|1"));

        assertThatThrownBy(() -> new ProductDaoText(file, categoryDao).findAll())
            .isInstanceOf(DataAccessException.class);
    }
}
