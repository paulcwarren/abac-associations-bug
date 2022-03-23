package com.example.abacassociationsbug;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import com.querydsl.core.types.Predicate;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.theoryinpractise.halbuilder.standard.StandardRepresentationFactory;
import java.util.stream.StreamSupport;
import lombok.Getter;
import lombok.Setter;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.CrudRepository;

import javax.persistence.*;

import java.io.StringReader;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static com.jayway.restassured.RestAssured.when;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@RunWith(Ginkgo4jSpringRunner.class)
@SpringBootTest(classes = {
        AbacAssociationsBugApplicationTests.Application.class }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AbacAssociationsBugApplicationTests {

    @LocalServerPort
    int port;

    @Autowired
    private ParentDocRepo parentDocRepo;

    @Autowired
    private ChildDocARepo childDocARepo;

    @Autowired
    private ChildDocBRepo childDocBRepo;

    long id ;

    {
        Describe("Hibernate investigation", () -> {

            Context("given a parent doc with a child doc A and a parent doc with child doc B", () -> {

                BeforeEach(() -> {
                    RestAssured.port = port;

                    ChildDocA a = new ChildDocA();
                    a.setName("child doc");
                    a = childDocARepo.save(a);

                    ParentDoc pdoc1 = new ParentDoc();
                    pdoc1.setName("parent-doc");
                    pdoc1.setBroker("foo");
                    pdoc1.setDocA(a);
                    pdoc1 = parentDocRepo.save(pdoc1);

                    ChildDocB b = new ChildDocB();
                    b.setName("child doc");
                    b = childDocBRepo.save(b);

                    ParentDoc pdoc2 = new ParentDoc();
                    pdoc2.setName("parent-doc");
                    pdoc2.setBroker("foo");
                    pdoc2.setDocB(b);
                    pdoc2 = parentDocRepo.save(pdoc2);

                    id = pdoc1.getId();
                });

//                It("should have the right structure", () -> {
//                    String res = when().get("/parentDocs?broker=foo").getBody().print();
//
//                    RepresentationFactory representationFactory = new StandardRepresentationFactory();
//                    ReadableRepresentation halResponse = representationFactory.readRepresentation("application/hal+json",new StringReader(res));
//                    assertThat(halResponse.getResources().size(), is(2));
//                });

                It("should handle searching across associations", () -> {
                    Predicate searchQuery = QAbacAssociationsBugApplicationTests_ParentDoc.parentDoc.docA.name.eq("child doc")
                                    .or(QAbacAssociationsBugApplicationTests_ParentDoc.parentDoc.docB.name.eq("child doc"));
                    Iterable<ParentDoc> result = parentDocRepo.findAll(searchQuery);
                    assertThat(StreamSupport.stream(result.spliterator(), false).count(), is(2));
                });
            });
        });
    }

    @SpringBootApplication
    @EnableJpaRepositories(considerNestedRepositories = true)
    public static class Application {
        public static void main(String[] args) {
            SpringApplication.run(Application.class, args);
        }
    }

    public interface ParentDocRepo extends CrudRepository<ParentDoc, Long>, QuerydslPredicateExecutor<ParentDoc> {}
    public interface ChildDocARepo extends CrudRepository<ChildDocA, Long>, QuerydslPredicateExecutor<ChildDocA> {}
    public interface ChildDocBRepo extends CrudRepository<ChildDocB, Long>, QuerydslPredicateExecutor<ChildDocB> {}

    @Entity
    @Getter
    @Setter
    public static class ParentDoc {
        
        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private Long id;

        private String name;
        private String broker;

        @OneToOne
        private ChildDocA docA;

        @OneToOne
        private ChildDocB docB;
    }

    @Entity
    @Getter
    @Setter
    public static class ChildDocA {

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private Long id;

        private String name;
    }

    @Entity
    @Getter
    @Setter
    public static class ChildDocB {

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private Long id;

        private String name;
    }

    @Test
    public void contextLoads() {
    }
}
