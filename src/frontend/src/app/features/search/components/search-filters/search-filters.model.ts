export type SearchFiltersValue = {
  gender: '' | 'male' | 'female';
  age_from: number | null;
  age_to: number | null;
  height_from: number | null;
  height_to: number | null;
  weight_from: number | null;
  weight_to: number | null;
  activity_years_from: number | null;
  activity_years_to: number | null;
  university_id: string | null;
  theatre: string;
  actor_rank: string;
  hair_color: string;
  eye_color: string;
  genre_drama: boolean;
  genre_comedy: boolean;
  genre_tragedy: boolean;
  genre_melodrama: boolean;
  genre_tragicomedy: boolean;
  genre_musical: boolean;
  genre_opera: boolean;
  genre_ballet: boolean;
  genre_monodrama: boolean;
};

export const DEFAULT_SEARCH_FILTERS: SearchFiltersValue = {
  gender: '',
  age_from: null,
  age_to: null,
  height_from: null,
  height_to: null,
  weight_from: null,
  weight_to: null,
  activity_years_from: null,
  activity_years_to: null,
  university_id: null,
  theatre: '',
  actor_rank: '',
  hair_color: '',
  eye_color: '',
  genre_drama: false,
  genre_comedy: false,
  genre_tragedy: false,
  genre_melodrama: false,
  genre_tragicomedy: false,
  genre_musical: false,
  genre_opera: false,
  genre_ballet: false,
  genre_monodrama: false,
};
