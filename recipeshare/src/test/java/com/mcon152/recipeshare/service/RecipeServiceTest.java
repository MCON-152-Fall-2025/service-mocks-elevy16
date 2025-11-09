package com.mcon152.recipeshare.service;

import com.mcon152.recipeshare.Recipe;
import com.mcon152.recipeshare.repository.RecipeRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Assignment: Implement all TODOs using Mockito features covered in class:
 *  - @Mock, @InjectMocks, @Captor, @ExtendWith(MockitoExtension.class)
 *  - Stubbing: thenReturn / thenAnswer / thenThrow
 *  - Verifications: verify(...), times/never/atLeast..., verifyNoMoreInteractions
 *  - InOrder (where meaningful)
 *  - Void stubbing: doNothing / doThrow (use deleteById for this)
 *  - Matchers: any(), eq(), argThat()
 *  - ArgumentCaptor
 *  - (Optional) Spy demo if you introduce a small helper in tests
 *
 * NOTE: This is a pure unit test. Do NOT start a Spring context.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RecipeService (Mockito) — Assignment Skeleton")
class RecipeServiceTest {

    @Mock
    private RecipeRepository recipeRepository;

    @InjectMocks
    private RecipeServiceImpl recipeService; // CUT implements RecipeService

    @Captor
    private ArgumentCaptor<Recipe> recipeCaptor;

    // --- Helpers for sample data ---

    private Recipe newRecipeNoId() {
        return new Recipe(
                null,
                "Chocolate Cake",
                "Moist chocolate cake",
                "flour, eggs, cocoa",
                "mix, bake",
                8
        );
    }

    private Recipe savedRecipe(long id) {
        return new Recipe(
                id,
                "Chocolate Cake",
                "Moist chocolate cake",
                "flour, eggs, cocoa",
                "mix, bake",
                8
        );
    }

    // ------------------ addRecipe ------------------

    @Nested
    @DisplayName("addRecipe(Recipe)")
    class AddRecipe {

        @Test
        @DisplayName("returns saved entity (thenReturn) and calls repository.save once")
        void returnsSaved_andSavesOnce() {
            // TODO:
            // 1) when(recipeRepository.save(...)).thenReturn(savedRecipe(1L))
            // 2) call recipeService.addRecipe(newRecipeNoId())
            // 3) assert non-null id and fields
            // 4) verify(recipeRepository).save(any(Recipe.class)); verifyNoMoreInteractions(recipeRepository)

            //See code below as an example answer

            Recipe input = newRecipeNoId();
            Recipe saved = savedRecipe(1L);

            when(recipeRepository.save(any(Recipe.class))).thenReturn(saved);

            Recipe out = recipeService.addRecipe(input);
            assertEquals(1L, out.getId());
            assertEquals(saved, out);

            verify(recipeRepository).save(any(Recipe.class));
            verifyNoMoreInteractions(recipeRepository);
        }

        @Test
        @DisplayName("assigns ID dynamically (thenAnswer) and captures argument")
        void assignsId_thenAnswer_andCaptures() {

            // Arrange
            // 1) Use thenAnswer to return a new Recipe with id=1L, copying fields from arg
            when(recipeRepository.save(any(Recipe.class))).thenAnswer(inv -> {
                Recipe r = inv.getArgument(0); // get the argument actually passed to save(...)
                return new Recipe(1L, r.getTitle(), r.getDescription(),
                        r.getIngredients(), r.getInstructions(), r.getServings());
            });

            // Act: call the service with new recipe
            Recipe out = recipeService.addRecipe(newRecipeNoId());

            // Assert: the returned entity has an id now
            assertEquals(1L, out.getId());

            // 2) capture the arg with ArgumentCaptor and assert title, id==null pre-save
            verify(recipeRepository).save(recipeCaptor.capture());
            Recipe sent = recipeCaptor.getValue();
            assertNull(sent.getId()); // before persistence
            assertEquals("Chocolate Cake", sent.getTitle());
        }

        @Test
        @DisplayName("propagates repository failure (thenThrow)")
        void propagatesRepositoryFailure() {
            // Arrange: simulate DB failure by throwing on save(...)
            when(recipeRepository.save(any(Recipe.class)))
                    .thenThrow(new IllegalStateException("DB down"));

            // Act and Assert: service should propagate
            assertThrows(IllegalStateException.class,
                    () -> recipeService.addRecipe(newRecipeNoId()));

            // Verifywhats
            verify(recipeRepository, times(1)).save(any(Recipe.class));
            verifyNoMoreInteractions(recipeRepository);

        }
    }

    // ------------------ getAllRecipes ------------------

    @Nested
    @DisplayName("getAllRecipes()")
    class GetAllRecipes {

        @Test
        @DisplayName("returns list from repository")
        void returnsList() {
            // Arrange
            List<Recipe> repoList = List.of(
                    new Recipe(1L, "Chocolate Chip Cookies", "moist cookies with chocolate chunks", "flour, sugar, oil, chocolate chips", "mix and bake", 12),
                    new Recipe(2L, "Kugel", "homey potato kugel", "potatoes, oil", "grate, mix, and bake", 10)
            );
            when(recipeRepository.findAll()).thenReturn(repoList);

            // Act
            List<Recipe> out = recipeService.getAllRecipes();

            // Assert: same content/size; and findAll called once
            assertEquals(2, out.size());
            assertEquals("Chocolate Chip Cookies", out.get(0).getTitle());
            assertEquals("Kugel", out.get(1).getTitle());
            verify(recipeRepository, times(1)).findAll();
            verifyNoMoreInteractions(recipeRepository);

        }
    }

    // ------------------ getRecipeById ------------------

    @Nested
    @DisplayName("getRecipeById(long)")
    class GetById {

        @Test
        @DisplayName("returns Optional.present when found")
        void present() {
            // Arrange
            when(recipeRepository.findById(1L)).thenReturn(Optional.of(savedRecipe(1L)));

            // Act
            Optional<Recipe> out = recipeService.getRecipeById(1L);

            // Assert
            assertTrue(out.isPresent());
            assertEquals(1L, out.get().getId());
            verify(recipeRepository).findById(1L);
            verifyNoMoreInteractions(recipeRepository);
        }

        @Test
        @DisplayName("returns Optional.empty when missing")
        void empty() {
            // Arrange
            when(recipeRepository.findById(999L)).thenReturn(Optional.empty());

            // Act
            Optional<Recipe> out = recipeService.getRecipeById(999L);

            // Assert
            assertTrue(out.isEmpty());
            verify(recipeRepository).findById(999L);
            verifyNoMoreInteractions(recipeRepository);
        }
    }

    // ------------------ deleteRecipe ------------------

    @Nested
    @DisplayName("deleteRecipe(long)")
    class DeleteRecipe {

        @Test
        @DisplayName("returns true when entity existed")
        void returnsTrue_whenExists() {
            long id = 10L;

            // Arrange: pretend the entity exists
            when(recipeRepository.existsById(id)).thenReturn(true);
            // deleteById is void; use doNothing to be explicit
            doNothing().when(recipeRepository).deleteById(id);

            // Act
            boolean result = recipeService.deleteRecipe(id);

            // Assert
            assertTrue(result);

            // Verify the methods were called in the correct order
            InOrder inOrder = inOrder(recipeRepository);
            inOrder.verify(recipeRepository).existsById(id);
            inOrder.verify(recipeRepository).deleteById(id);
            verifyNoMoreInteractions(recipeRepository); // nothing else on the mock was used
        }

        @Test
        @DisplayName("returns false when missing (never deletes)")
        void returnsFalse_whenMissing() {
            long id = 11L;

            // Arrange: pretend the method is missing
            when(recipeRepository.existsById(id)).thenReturn(false);

            // Act
            boolean result = recipeService.deleteRecipe(id);

            // Assert
            assertFalse(result);
            verify(recipeRepository, times(1)).existsById(id);
            verify(recipeRepository, never()).deleteById(anyLong());
            verifyNoMoreInteractions(recipeRepository);
        }

        @Test
        @DisplayName("propagates delete error (doThrow)")
        void propagatesDeleteError() {
            long id = 12L;

            // Arrange: exists, but delete operation throws
            when(recipeRepository.existsById(id)).thenReturn(true);
            doThrow(new IllegalStateException("cannot delete"))
                    .when(recipeRepository).deleteById(id);

            // Act and Assert
            assertThrows(IllegalStateException.class, () -> recipeService.deleteRecipe(id));

            // Verify the order
            InOrder inOrder = inOrder(recipeRepository);
            inOrder.verify(recipeRepository).existsById(id);
            inOrder.verify(recipeRepository).deleteById(id);
            verifyNoMoreInteractions(recipeRepository);
        }
    }

    // ------------------ updateRecipe ------------------

    @Nested
    @DisplayName("updateRecipe(long, Recipe)")
    class UpdateRecipe {

        @Test
        @DisplayName("returns updated entity when exists")
        void returnsUpdated_whenExists() {
            long id = 20L;

            // Arrange existing record
            Recipe existing = new Recipe(id, "Old", "old d", "old i", "old steps", 2);
            when(recipeRepository.findById(id)).thenReturn(Optional.of(existing));

            // Assert what we pass to save(...), and also return the saved entity.
            when(recipeRepository.save(any(Recipe.class))).thenAnswer(inv -> inv.getArgument(0));

            // Build update payload
            Recipe update = new Recipe(null, "New", "new d", "new i", "new steps", 4);

            // Act
            Optional<Recipe> outOpt = recipeService.updateRecipe(id, update);

            // Assert
            assertTrue(outOpt.isPresent());
            Recipe out = outOpt.get();
            assertEquals(id, out.getId());
            assertEquals("New", out.getTitle());
            assertEquals("new d", out.getDescription());
            assertEquals("new i", out.getIngredients());
            assertEquals("new steps", out.getInstructions());
            assertEquals(4, out.getServings());

            // Verify and capture the actual entity sent to save(...)
            verify(recipeRepository).findById(id);
            verify(recipeRepository).save(recipeCaptor.capture());
            Recipe sent = recipeCaptor.getValue();
            assertEquals(id, sent.getId());
            assertEquals("New", sent.getTitle());
            assertEquals("new d", sent.getDescription());
            assertEquals("new i", sent.getIngredients());
            assertEquals("new steps", sent.getInstructions());
            assertEquals(4, sent.getServings());
            verifyNoMoreInteractions(recipeRepository);
         }

        @Test
        @DisplayName("returns empty when entity missing")
        void returnsEmpty_whenMissing() {
            long id = 21L;

            // Arrange: record not found
            when(recipeRepository.findById(id)).thenReturn(Optional.empty());

            // Act
            Optional<Recipe> out = recipeService.updateRecipe(id, newRecipeNoId());

            // Assert
            assertTrue(out.isEmpty());
            verify(recipeRepository).findById(id);
            verify(recipeRepository, never()).save(any());
            verifyNoMoreInteractions(recipeRepository);
        }
    }

    // ------------------ patchRecipe ------------------

    @Nested
    @DisplayName("patchRecipe(long, Recipe)")
    class PatchRecipe {

        @Test
        @DisplayName("applies only non-null fields (argThat)")
        void appliesNonNullFields_only() {
            long id = 30L;

            // Existing record
            Recipe existing = new Recipe(id, "T1", "D1", "I1", "S1", 2);
            when(recipeRepository.findById(id)).thenReturn(Optional.of(existing));

            // Save echoes argument
            when(recipeRepository.save(any(Recipe.class))).thenAnswer(inv -> inv.getArgument(0));

            // Patch only the title
            Recipe patch = new Recipe(null, "T2", null, null, null, null);

            // Act
            Optional<Recipe> outOpt = recipeService.patchRecipe(id, patch);

            // Assert
            assertTrue(outOpt.isPresent());

            // Verify that saved entity changed title but kept other fields intact
            verify(recipeRepository).findById(id);
            verify(recipeRepository).save(argThat(r ->
                    r.getId().equals(id) &&
                            r.getTitle().equals("T2") &&
                            r.getDescription().equals("D1") &&
                            r.getIngredients().equals("I1") &&
                            r.getInstructions().equals("S1") &&
                            r.getServings().equals(2)
            ));
            verifyNoMoreInteractions(recipeRepository);
        }

        @Test
        @DisplayName("returns empty when entity missing")
        void returnsEmpty_whenMissing() {
            long id = 31L;

            // Arrange
            when(recipeRepository.findById(id)).thenReturn(Optional.empty());

            // Act
            Optional<Recipe> out = recipeService.patchRecipe(id, new Recipe(null, "X", null, null, null, null));

            // Assert
            assertTrue(out.isEmpty());
            verify(recipeRepository).findById(id);
            verify(recipeRepository, never()).save(any());
            verifyNoMoreInteractions(recipeRepository);
         }
    }

    // ------------------ extra practice ------------------

    @Nested
    @DisplayName("Advanced stubbing & verification")
    class Advanced {

        @Test
        @DisplayName("consecutive stubs on existsById (true, false)")
        void consecutiveStubs_existsById() {
            long id = 40L;

            // Consecutive stubbing: first call returns true, second call returns false.
            when(recipeRepository.existsById(id)).thenReturn(true, false);

            // Act #1
            boolean first = recipeRepository.existsById(id);
            // Act #2
            boolean second = recipeRepository.existsById(id);

            // Assert
            assertTrue(first);
            assertFalse(second);

            // Verify exactly two calls and nothing else
            verify(recipeRepository, times(2)).existsById(id);
            verifyNoMoreInteractions(recipeRepository);
         }
    }
}
