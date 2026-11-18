package com.maisonglow.integration;

import com.maisonglow.AppContext;
import com.maisonglow.ai.AiConfig;
import com.maisonglow.service.LocalSeedService;
import com.maisonglow.service.LocalSeedService.Profile;
import java.nio.file.*;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.*;

class LocalSeedServiceTest {
    @TempDir Path root;
    private static final LocalDateTime REFERENCE=LocalDateTime.of(2026,11,17,12,0);
    private static char[] password(){return "fixture-password-only".toCharArray();}

    @Test void smallFixtureReopensWithAccountsOrdersAndHistory()throws Exception {
        Path path=root.resolve("demo");var service=new LocalSeedService();
        var report=service.generate(path,Profile.SMALL,REFERENCE,42,password());
        assertThat(report.counts()).containsEntry("customer",12L).containsEntry("app_user",3L)
            .containsEntry("product",12L).containsEntry("appointment",36L).containsEntry("sale",24L)
            .containsEntry("customer_order",12L).containsEntry("customer_change",12L);
        assertThat(LocalSeedService.openDemoFolder(path)).isEqualTo(path);
        var app=new AppContext(path,Clock.fixed(REFERENCE.toInstant(ZoneOffset.UTC),ZoneOffset.UTC),AiConfig.simulated());
        app.auth().login("demo-cliente-0001",password());
        assertThat(app.orders().mine()).isNotEmpty();
        assertThat(app.changes().mine()).isNotEmpty();
        app.auth().login("demo-admin-1",password());
        assertThat(app.orders().all()).hasSize(12);
        var again=service.generate(path,Profile.SMALL,REFERENCE,42,password());
        assertThat(again.reused()).isTrue();
        assertThat(again.counts()).isEqualTo(report.counts());
        assertThat(Files.readString(path.resolve("seed-manifest.txt"))).doesNotContain(new String(password()));
    }

    @Test void refusesExistingDataAndDifferentSeed()throws Exception {
        var service=new LocalSeedService();Path path=Files.createDirectory(root.resolve("business"));
        Files.writeString(path.resolve("customers.txt"),"existing");
        assertThatThrownBy(()->service.generate(path,Profile.SMALL,REFERENCE,42,password())).isInstanceOf(com.maisonglow.exception.ValidationException.class);
        assertThat(Files.readString(path.resolve("customers.txt"))).isEqualTo("existing");
        Path demo=root.resolve("demo");service.generate(demo,Profile.SMALL,REFERENCE,42,password());
        assertThatThrownBy(()->service.generate(demo,Profile.SMALL,REFERENCE,43,password())).isInstanceOf(com.maisonglow.exception.ValidationException.class);
        assertThat(service.generate(demo,Profile.SMALL,REFERENCE,42,password()).counts()).containsEntry("customer",12L);
    }

    @Test void largeFixtureHasPlannedVolumes()throws Exception {
        var counts=new LocalSeedService().generate(root.resolve("large"),Profile.LARGE,REFERENCE,42,password()).counts();
        assertThat(counts).containsEntry("customer",1000L).containsEntry("product",200L)
            .containsEntry("professional",20L).containsEntry("treatment",30L)
            .containsEntry("appointment",5000L).containsEntry("sale",3000L)
            .containsEntry("customer_order",1000L).containsEntry("customer_change",1000L);
    }
}
