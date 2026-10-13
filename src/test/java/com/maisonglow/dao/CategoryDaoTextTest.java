package com.maisonglow.dao;

import com.maisonglow.exception.DataAccessException;
import com.maisonglow.model.ProductCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CategoryDaoText tests")
class CategoryDaoTextTest {

    @TempDir
    Path folder;

    @Test
    @DisplayName("created categories survive a reload from disk")
    void createAndReload() throws DataAccessException {
        Path file = folder.resolve("categories.txt");
        new CategoryDaoText(file).create(new ProductCategory(0, "Cabello"));
        new CategoryDaoText(file).create(new ProductCategory(0, "Uñas | manos"));

        CategoryDao reloaded = new CategoryDaoText(file);

        assertThat(reloaded.findAll()).extracting(ProductCategory::getName)
            .containsExactly("Cabello", "Uñas | manos");
        assertThat(reloaded.findById(2)).map(ProductCategory::getName).contains("Uñas | manos");
    }

    @Test
    @DisplayName("the preloaded categories file has the seed categories")
    void preloadedFile() throws DataAccessException {
        CategoryDao dao = new CategoryDaoText(Path.of("data", "categories.txt"));

        assertThat(dao.findAll()).extracting(ProductCategory::getName)
            .containsExactly("Cabello", "Piel", "Uñas", "Maquillaje", "Accesorios");
    }
}
