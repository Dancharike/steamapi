package lt.viko.eif.kladijev.steamapi.resources;

import lt.viko.eif.kladijev.steamapi.dto.AchievementDto;
import lt.viko.eif.kladijev.steamapi.dto.GameDto;
import lt.viko.eif.kladijev.steamapi.dto.ItemDto;
import lt.viko.eif.kladijev.steamapi.mappers.AchievementMapper;
import lt.viko.eif.kladijev.steamapi.mappers.GameMapper;
import lt.viko.eif.kladijev.steamapi.mappers.ItemMapper;
import lt.viko.eif.kladijev.steamapi.models.Game;
import lt.viko.eif.kladijev.steamapi.repositories.GameRepository;
import lt.viko.eif.kladijev.steamapi.utility.NotFoundException;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

/**
 * Класс контроллер для управления играми.
 * Присутствуют CRUD-методы и HATEOAS ссылки.
 */
@RestController
@RequestMapping("api/games")
public class GameResource
{
    private final GameRepository gameRepository;

    public GameResource(GameRepository gameRepository)
    {
        this.gameRepository = gameRepository;
    }

    /**
     * Метод для получения списка всех игр.
     * @return коллекция DTO игр со ссылками.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CollectionModel<EntityModel<GameDto>> getAllGames()
    {
        var games = gameRepository.findAll().stream()
                .map(game -> {
                    GameDto dto = GameMapper.toDto(game);
                    Long gameId = game.getId();

                    var model = EntityModel.of(dto);
                    model.add(linkTo(methodOn(GameResource.class).getGameById(gameId)).withSelfRel());
                    model.add(linkTo(methodOn(GameResource.class).updateGame(gameId, null)).withRel("update"));
                    model.add(linkTo(methodOn(GameResource.class).deleteGame(gameId)).withRel("delete"));
                    model.add(linkTo(methodOn(GameResource.class).getGameAchievementsByID(gameId)).withRel("achievements"));
                    model.add(linkTo(methodOn(GameResource.class).getGameItemsByID(gameId)).withRel("items"));

                    return model;
                }).toList();

        return CollectionModel.of(games,
                linkTo(methodOn(GameResource.class).getAllGames()).withSelfRel(),
                linkTo(methodOn(GameResource.class).createGame(null)).withRel("create"));
    }

    /**
     * Метод для получения игры со специфическим ID.
     * @param id ID игры.
     * @return DTO игры с HATEOAS ссылками.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public EntityModel<GameDto> getGameById(@PathVariable Long id)
    {
        Game game = gameRepository.findById(id).orElseThrow(() -> new NotFoundException("Game", id));
        GameDto dto = GameMapper.toDto(game);

        var model = EntityModel.of(dto);
        model.add(linkTo(methodOn(GameResource.class).getGameById(id)).withSelfRel());
        model.add(linkTo(methodOn(GameResource.class).updateGame(id, null)).withRel("update"));
        model.add(linkTo(methodOn(GameResource.class).deleteGame(id)).withRel("delete"));
        model.add(linkTo(methodOn(GameResource.class).getGameAchievementsByID(id)).withRel("achievements"));
        model.add(linkTo(methodOn(GameResource.class).getGameItemsByID(id)).withRel("items"));
        model.add(linkTo(methodOn(GameResource.class).getAllGames()).withRel("all-games"));

        return model;
    }

    /**
     * Метод для получения достижений, что привязаны к игре.
     * @param id ID игры.
     * @return список DTO достижений.
     */
    @GetMapping("/{id}/achievements")
    @PreAuthorize("hasRole('ADMIN')")
    public CollectionModel<EntityModel<AchievementDto>> getGameAchievementsByID(@PathVariable Long id)
    {
        Game game = gameRepository.findById(id).orElseThrow(() -> new NotFoundException("Game", id));

        List<EntityModel<AchievementDto>> achievements = game.getAchievements().stream()
                .map(a -> {
                    AchievementDto dto = AchievementMapper.toDto(a);
                    return EntityModel.of(dto,
                            linkTo(methodOn(AchievementResource.class).getAchievementById(a.getId())).withSelfRel(),
                            linkTo(methodOn(AchievementResource.class).updateAchievement(a.getId(), null)).withRel("update"),
                            linkTo(methodOn(AchievementResource.class).deleteAchievement(a.getId())).withRel("delete"));
                }).toList();

        return CollectionModel.of(achievements, linkTo(methodOn(GameResource.class).getGameAchievementsByID(id)).withSelfRel());
    }

    /**
     * Метод для получения предметов, что привязаны к игре.
     * @param id ID предмета.
     * @return список DTO предметов.
     */
    @GetMapping("/{id}/items")
    @PreAuthorize("hasRole('ADMIN')")
    public CollectionModel<EntityModel<ItemDto>> getGameItemsByID(@PathVariable Long id)
    {
        Game game = gameRepository.findById(id).orElseThrow(() -> new NotFoundException("Game", id));

        List<EntityModel<ItemDto>> items = game.getItems().stream()
                .map(item -> {
                    ItemDto dto = ItemMapper.toDto(item);
                    return EntityModel.of(dto,
                            linkTo(methodOn(ItemResource.class).getItemById(item.getId())).withSelfRel(),
                            linkTo(methodOn(ItemResource.class).updateItem(item.getId(), null)).withRel("update"),
                            linkTo(methodOn(ItemResource.class).deleteItem(item.getId())).withRel("delete"));
                }).toList();

        return CollectionModel.of(items, linkTo(methodOn(GameResource.class).getGameItemsByID(id)).withSelfRel());
    }

    /**
     * Метод для нахождения достижений по названию игры.
     * @param name название игры.
     * @return список DTO предметов.
     */
    @GetMapping("/name/{name}/achievements")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLAYER')")
    public List<AchievementDto> getGameAchievementsByName(@PathVariable String name)
    {
        Game game = gameRepository.findByGameTitle(name).orElseThrow(() -> new NotFoundException("Game", name));

        return game.getAchievements().stream()
                .map(AchievementMapper::toDto)
                .toList();
    }

    /**
     * Метод для нахождения предметов по названию игры.
     * @param name название игры.
     * @return список DTO предметов.
     */
    @GetMapping("/name/{name}/items")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLAYER')")
    public List<ItemDto> getGameItemsByName(@PathVariable String name)
    {
        Game game = gameRepository.findByGameTitle(name).orElseThrow(() -> new NotFoundException("Game", name));

        return game.getItems().stream()
                .map(ItemMapper::toDto)
                .toList();
    }

    /**
     * Метод для создания новой игры.
     * @param game объект игры.
     * @return созданная игра со ссылками.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public EntityModel<GameDto> createGame(@RequestBody Game game)
    {
        Game saved = gameRepository.save(game);
        GameDto dto = GameMapper.toDto(saved);

        return EntityModel.of(dto,
                linkTo(methodOn(GameResource.class).getGameById(saved.getId())).withSelfRel(),
                linkTo(methodOn(GameResource.class).updateGame(saved.getId(), null)).withRel("update"),
                linkTo(methodOn(GameResource.class).deleteGame(saved.getId())).withRel("delete"),
                linkTo(methodOn(GameResource.class).getGameAchievementsByID(saved.getId())).withRel("achievements"),
                linkTo(methodOn(GameResource.class).getGameItemsByID(saved.getId())).withRel("items"),
                linkTo(methodOn(GameResource.class).getAllGames()).withRel("all-games"));
    }

    /**
     * Метод для обновления информации об игре.
     * @param id ID игры.
     * @param updated новые данные
     * @return обновлённая игра со ссылками.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public EntityModel<GameDto> updateGame(@PathVariable Long id, @RequestBody Game updated)
    {
        Game existing = gameRepository.findById(id).orElseThrow(() -> new RuntimeException("Game not found"));

        existing.setGameTitle(updated.getGameTitle());
        existing.setGameGenre(updated.getGameGenre());
        existing.setGameDescription(updated.getGameDescription());

        Game saved = gameRepository.save(existing);
        GameDto dto = GameMapper.toDto(saved);

        return EntityModel.of(dto,
                linkTo(methodOn(GameResource.class).getGameById(saved.getId())).withSelfRel(),
                linkTo(methodOn(GameResource.class).deleteGame(saved.getId())).withRel("delete"),
                linkTo(methodOn(GameResource.class).getGameAchievementsByID(saved.getId())).withRel("achievements"),
                linkTo(methodOn(GameResource.class).getGameItemsByID(saved.getId())).withRel("items"),
                linkTo(methodOn(GameResource.class).getAllGames()).withRel("all-games"));
    }

    /**
     * Метод для удаления игры со специфическим ID.
     * @param id ID игры.
     * @return код 204 No Content.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteGame(@PathVariable Long id)
    {
        gameRepository.deleteById(id);

        var model = EntityModel.of("Game deleted successfully!");
        model.add(linkTo(methodOn(GameResource.class).getAllGames()).withRel("all-games"));
        model.add(linkTo(methodOn(GameResource.class).createGame(null)).withRel("create"));

        //return ResponseEntity.noContent().build();
        return ResponseEntity.ok(model);
    }
}
