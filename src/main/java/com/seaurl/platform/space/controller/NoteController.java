package com.seaurl.platform.space.controller;

import com.seaurl.common.result.ResultStatus;
import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultSuccess;
import com.seaurl.platform.common.context.UserContext;
import com.seaurl.platform.common.context.UserContextHolder;

import com.seaurl.platform.common.model.LoginUser;
import com.seaurl.platform.common.model.Note;
import com.seaurl.platform.common.model.Url;
import com.seaurl.platform.common.model.User;
import com.seaurl.platform.space.request.NoteRequest;
import com.seaurl.platform.space.request.UrlRequest;
import com.seaurl.platform.space.service.NoteCategoryService;
import com.seaurl.platform.space.service.NoteService;
import com.seaurl.platform.space.service.StorageService;
import com.seaurl.platform.space.service.UserService;
import com.seaurl.platform.space.service.facade.UsersFacade;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/space")

@Observed
public class NoteController {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    UsersFacade usersFacade;

    @Autowired
    UserService userService;

    @Autowired
    NoteCategoryService noteCategoryService;

    @Autowired
    NoteService noteService;

    @Autowired
    StorageService storageService;


    /**
     * @param type
     * @return
     */
    @GetMapping("/crud/url/getNotesListByType")
    public ResponseEntity<?> getNotesListByType(@RequestParam String type) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }

            if (!StringUtils.hasLength(type)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }


            if (type.equals("frequently")) {
                List<Note> result = noteService.getRecentlyNotes(loginUser.getUid());
                return ResponseEntity.ok(new ResultSuccess<>(result));
            }

            if (type.equals("favorite")) {
                List<Note> result = noteService.queryIsFavoriteList(loginUser.getUid());
                return ResponseEntity.ok(new ResultSuccess<>(result));
            }

            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        } catch (Exception ex) {
            log.error("getRecentlyNotes exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    @GetMapping("/crud/url/getNotesCountByType")
    public ResponseEntity<?> getNotesCountByType() {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }

            // frequently
            Object r1 = noteService.getRecentlyNotesCount(loginUser.getUid());
            //favorite
            Object r2 = noteService.queryIsFavoriteCount(loginUser.getUid());

            HashMap<String, Object> result = new HashMap<>();

            result.put("frequently", r1);
            result.put("favorite", r2);

            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("getRecentlyNotes exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 获取最近使用的笔记
     *
     * @return
     */
    @GetMapping("/crud/note/search")
    public ResponseEntity<?> searchNotes(@RequestParam(value = "keyword", required = false) String keyword) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            log.info("searchNotes uid={}, keyword={}", loginUser.getUid(), keyword);
            List<Note> result = noteService.searchNotes(loginUser.getUid(), keyword);
            log.info("searchNotes result count={}", result.size());
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("searchNotes exception={}", ex.getMessage(), ex);
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    @GetMapping("/crud/note/getRecentlyNotes")
    public ResponseEntity<?> getRecentlyNotes() {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            List<Note> result = noteService.getRecentlyNotes(loginUser.getUid());
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("getRecentlyNotes exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * @return
     */
    @PostMapping(value = "/info/note/editor/upload/image")
    public Mono<Map<String, String>> uploadImage(@RequestPart("upload") Mono<FilePart> fileMono) {
        Map<String, String> result = new HashMap<>();

        return fileMono.flatMap(file -> {
            try {
                if (file == null || "".equals(file.filename().trim())) {
                    result.put("uploaded", "false");
                    result.put("url", "#");
                    return Mono.just(result);
                }
                return storageService.uploadImgByInputStreamNoBlock(file, "space/note/editor/images/" + System.currentTimeMillis())
                        .map(endpath -> {
                            result.put("uploaded", "true");
                            result.put("url", endpath);
                            return result;
                        })
                        .onErrorResume(ex -> {
                            log.info("uploadImage ex={}", ex.getMessage());
                            result.put("uploaded", "false");
                            result.put("url", "#");
                            return Mono.just(result);
                        });
            } catch (Exception ex) {
                log.error("uploadImage exception={}", ex.getMessage());
                result.put("uploaded", "false");
                result.put("url", "#");
                return Mono.just(result);
            }
        });
    }

    /**
     * @param noteRequest
     * @return
     */
    @PostMapping("/crud/note/add")
    public Mono<ResponseEntity<?>> add(@RequestBody NoteRequest noteRequest) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND)));
        }
        String title = noteRequest.getTitle();
        String cid = noteRequest.getCategoryId();
        if (!StringUtils.hasLength(title) || !StringUtils.hasLength(cid)) {
            return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY)));
        }
        try {
            Date current = new Date();
            Mono<User> userMono = usersFacade.getAccountInfoByUseId(loginUser.getUid());
            return userMono.map(user -> {
                Note note = new Note();
                note.setUid(user.getUid());
                note.setTitle(title);
                note.setCategoryId(cid);
                note.setIsPrivate(false);
                note.setIsDelete(false);
                note.setCreatedDt(current);
                note.setUpdatedDt(current);
                boolean result = false;
                try {
                    result = noteService.add(note);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return ResponseEntity.ok(new ResultSuccess<>(result));
            });

        } catch (Exception ex) {
            log.error("保存Note出错-{}", ex.getMessage());
            return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception)));
        }
    }

    /**
     * 为ai添加笔记
     *
     * @param noteRequest
     * @return
     */
    @PostMapping("/crud/note/addForAi")
    public Mono<ResponseEntity<?>> addForAi(@RequestBody NoteRequest noteRequest) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND)));
        }
        String title = noteRequest.getTitle();
        String cid = noteRequest.getCategoryId();
        String content = noteRequest.getContent();
        if (!StringUtils.hasLength(title) || !StringUtils.hasLength(cid) || !StringUtils.hasLength(content)) {
            return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY)));
        }
        try {
            Date current = new Date();
            Mono<User> userMono = usersFacade.getAccountInfoByUseId(loginUser.getUid());
            return userMono.map(user -> {
                Note note = new Note();
                note.setUid(user.getUid());
                note.setTitle(title);
                note.setCategoryId(cid);
                note.setContent(content);
                note.setIsPrivate(false);
                note.setIsDelete(false);
                note.setCreatedDt(current);
                note.setUpdatedDt(current);
                boolean result = false;
                try {
                    result = noteService.add(note);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return ResponseEntity.ok(new ResultSuccess<>(result));
            });

        } catch (Exception ex) {
            log.error("保存Note出错-{}", ex.getMessage());
            return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception)));
        }
    }


    /**
     * @param noteRequest
     * @return
     */
    @PostMapping("/crud/note/edit/title")
    public ResponseEntity<?> editTitle(@RequestBody NoteRequest noteRequest) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            String noteId = noteRequest.getNoteId();
            String title = noteRequest.getTitle();
            if (!StringUtils.hasLength(noteId) || !StringUtils.hasLength(title)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            Note note = new Note();
            note.setId(noteId);
            note.setTitle(title);
            note.setUpdatedDt(new Date());
//        note.setCategoryId(categoryId);
            boolean result = noteService.editTitle(note);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("editTitle exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }

    }

    /**
     * @param noteRequest
     * @return
     */
    @PostMapping("/crud/note/edit/content")
    public ResponseEntity<?> editContent(@RequestBody NoteRequest noteRequest) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }

            String noteId = noteRequest.getNoteId();
            String content = noteRequest.getContent();
            if (!StringUtils.hasLength(noteId)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            Note prev = noteService.queryNoteById(noteId);
            if (prev != null && StringUtils.hasLength(prev.getContent()) && prev.getContent().equals(content)) {
                //相同则不保存
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.SAME_DATA));
            } else {
                Note note = new Note();
                note.setId(noteId);
                note.setContent(content);
                note.setUpdatedDt(new Date());
                boolean result = noteService.editContent(note);
                note.setContent(content.length() <= 100 ? content : content.substring(0, 100));
                if (result) {
                    Note note1 = noteService.queryNoteById(noteId);
                    return ResponseEntity.ok(new ResultSuccess<>(note1));
                }
            }
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.UPDATE_ERROR));
        } catch (Exception ex) {
            log.error("editContent exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * @param noteRequest
     * @return
     */
    @PostMapping("/crud/note/edit/categoryId")
    public ResponseEntity<?> editCategoryId(@RequestBody NoteRequest noteRequest) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }

            String noteId = noteRequest.getNoteId();
            String categoryId = noteRequest.getCategoryId();
            if (!StringUtils.hasLength(categoryId)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            Note note = new Note();
            note.setId(noteId);
            note.setCategoryId(categoryId);
            note.setUpdatedDt(new Date());
            boolean result = noteService.editCategoryId(note);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("editCategoryId exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * @param noteId
     * @return
     */
    @DeleteMapping("/crud/note/delete/{noteId}")
    public ResponseEntity<?> delete(@PathVariable String noteId) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }

        if (!StringUtils.hasLength(noteId)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        try {
            boolean result = noteService.delete(noteId);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("获取domain出错-{}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 根据categoryId查询笔记列表
     *
     * @param cid
     * @return
     */
    @GetMapping("/crud/note/queryListBy/{cid}")
    public ResponseEntity<?> getNoteByCategoryId(@PathVariable String cid) {
        if (!StringUtils.hasLength(cid)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }

        try {
            List<Note> notes = noteService.queryNoteByCategoryId(cid);
            return ResponseEntity.ok(new ResultSuccess<>(notes));
        } catch (Exception ex) {
            log.error("getNoteByCategoryId exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * @param noteId
     * @return
     */
    @GetMapping("/crud/note/queryBy/{noteId}")
    public ResponseEntity<?> queryContent(@PathVariable String noteId) {
        try {
            if (!StringUtils.hasLength(noteId)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            Note note = noteService.queryNoteById(noteId);
            return ResponseEntity.ok(new ResultSuccess<>(note));
        } catch (Exception ex) {
            log.error("queryContent exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 设置是否私有
     *
     * @param noteRequest
     * @return
     */
    @PostMapping("/crud/note/setPrivate")
    public ResponseEntity<?> setPrivate(@RequestBody NoteRequest noteRequest) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }
            String noteId = noteRequest.getNoteId();
            Boolean isPrivate = noteRequest.getIsPrivate();
            if (!StringUtils.hasLength(noteId)) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }
            boolean result = noteService.editIsPrivate(noteId, isPrivate);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("setPrivate exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }


    /**
     * 设置是否收藏
     *
     * @param urlRequest
     * @return
     */
    @PostMapping("/crud/note/setFavorite")
    public ResponseEntity<?> setFavorite(@RequestBody NoteRequest urlRequest) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }

        String noteId = urlRequest.getNoteId();
        boolean isFavorite = urlRequest.getIsFavorite();

        if (!StringUtils.hasLength(noteId)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }

        try {
            boolean result = noteService.setFavorite(noteId, isFavorite);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.info("setFavorite ex={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }
}
