import request from "../utils/request";

export const getFeed = (params) => request.get("/content/feed", { params });
export const getNoteDetail = (id) => request.get(`/content/notes/${id}`);
export const publishNote = (data) => request.post("/content/notes", data);
export const login = (data) => request.post("/user/login", data);
export const getUserProfile = (id) => request.get(`/user/${id}`);
export const likeNote = (id) => request.post(`/interact/like/${id}`);
export const collectNote = (id) => request.post(`/interact/collect/${id}`);
