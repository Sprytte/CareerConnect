import { useEffect, useMemo, useState } from "react";
import "../styles/SearchJobs.css";
import SiteHeader from "./SiteHeader";
import SiteFooter from "./SiteFooter";
import { backendUrl } from "../constants";

type JobPosting = {
  id: number;
  userId: string;
  jobName: string;
  company: string;
  description: string;
  location: string;
  category: string;
  salary: string;
  dateCreated: string;
  deadline: string;
};

export default function SearchJobs() {
  const [jobs, setJobs] = useState<JobPosting[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState("");

  const [keyword, setKeyword] = useState("");
  const [location, setLocation] = useState("");
  const [category, setCategory] = useState("");
  const [expandedJobId, setExpandedJobId] = useState<number | null>(null);

  useEffect(() => {
    const loadJobs = async () => {
      try {
        setIsLoading(true);
        setLoadError("");

        const response = await fetch(
          `${backendUrl}/api/v1/job-postings`,
          {
            credentials: "include",
          }
        );

        if (!response.ok) {
          throw new Error("Failed to load jobs");
        }

        const data: JobPosting[] = await response.json();
        setJobs(data);
      } catch (error) {
        console.error("Error loading job postings:", error);
        setLoadError(
          "Job postings could not be loaded. Please try again later."
        );
      } finally {
        setIsLoading(false);
      }
    };

    loadJobs();
  }, []);

  const categories = Array.from(
    new Set(jobs.map((job) => job.category))
  );

  const filteredJobs = useMemo(() => {
    const keywordValue = keyword.trim().toLowerCase();
    const locationValue = location.trim().toLowerCase();

    return jobs.filter((job) => {
      const matchesKeyword =
        keywordValue === "" ||
        job.jobName.toLowerCase().includes(keywordValue) ||
        job.company.toLowerCase().includes(keywordValue) ||
        job.description.toLowerCase().includes(keywordValue);

      const matchesLocation =
        locationValue === "" ||
        job.location.toLowerCase().includes(locationValue);

      const matchesCategory =
        category === "" || job.category === category;

      return matchesKeyword && matchesLocation && matchesCategory;
    });
  }, [jobs, keyword, location, category]);

  const clearFilters = () => {
    setKeyword("");
    setLocation("");
    setCategory("");
  };

  const formatDate = (date: string) => {
    if (!date) {
      return "N/A";
    }

    const dateOnly = date.substring(0, 10);
    const [year, month, day] = dateOnly.split("-");

    return new Date(
      Number(year),
      Number(month) - 1,
      Number(day)
    ).toLocaleDateString();
  };

  return (
    <div className="search-jobs-page">
      <SiteHeader />

      <main className="search-jobs-main">
        <section className="search-jobs-heading">
          <span className="search-jobs-kicker">
            FIND YOUR NEXT MOVE
          </span>

          <h1>Search for Jobs</h1>

          <p>
            Explore available opportunities and narrow your search by
            keyword, location, or category.
          </p>
        </section>

        <section className="job-search-filters">
          <div className="job-search-field">
            <label htmlFor="job-keyword">Keyword</label>

            <input
              id="job-keyword"
              type="text"
              placeholder="Job title, company, or keyword"
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
            />
          </div>

          <div className="job-search-field">
            <label htmlFor="job-location">Location</label>

            <input
              id="job-location"
              type="text"
              placeholder="Montreal, Toronto..."
              value={location}
              onChange={(event) => setLocation(event.target.value)}
            />
          </div>

          <div className="job-search-field">
            <label htmlFor="job-category">Category</label>

            <select
              id="job-category"
              value={category}
              onChange={(event) => setCategory(event.target.value)}
            >
              <option value="">All categories</option>

              {categories.map((jobCategory) => (
                <option key={jobCategory} value={jobCategory}>
                  {jobCategory}
                </option>
              ))}
            </select>
          </div>

          <button
            className="clear-filters-button"
            type="button"
            onClick={clearFilters}
          >
            Clear filters
          </button>
        </section>

        <div className="search-results-heading">
          <h2>Available Jobs</h2>

          {!isLoading && !loadError && (
            <span>
              {filteredJobs.length}{" "}
              {filteredJobs.length === 1 ? "result" : "results"}
            </span>
          )}
        </div>

        {isLoading ? (
          <section className="search-jobs-empty">
            <h2>Loading jobs...</h2>
            <p>Please wait while available opportunities are loaded.</p>
          </section>
        ) : loadError ? (
          <section className="search-jobs-empty">
            <h2>Unable to load jobs</h2>
            <p>{loadError}</p>
          </section>
        ) : filteredJobs.length === 0 ? (
          <section className="search-jobs-empty">
            <h2>No jobs found</h2>
            <p>
              Try changing your keyword, location, or category.
            </p>
          </section>
        ) : (
          <section className="search-jobs-grid">
            {filteredJobs.map((job) => {
              const isExpanded = expandedJobId === job.id;

              return (
                <article className="search-job-card" key={job.id}>
                  <span className="search-job-category">
                    {job.category}
                  </span>

                  <h2>{job.jobName}</h2>

                  <div className="search-job-details">
                    <span>{job.company}</span>
                    <span>•</span>
                    <span>{job.location}</span>

                    {job.salary && (
                      <>
                        <span>•</span>
                        <span>{job.salary}</span>
                      </>
                    )}
                  </div>

                  <p
                    className={
                      isExpanded
                        ? "search-job-description expanded"
                        : "search-job-description"
                    }
                  >
                    {job.description}
                  </p>

                  <div className="search-job-footer">
                    <div className="search-job-dates">
                      <span>
                        Posted {formatDate(job.dateCreated)}
                      </span>

                      <span>
                        Deadline {formatDate(job.deadline)}
                      </span>
                    </div>

                    <button
                      className="view-details-button"
                      type="button"
                      onClick={() =>
                        setExpandedJobId(
                          isExpanded ? null : job.id
                        )
                      }
                    >
                      {isExpanded ? "Hide details" : "View details"}
                    </button>
                  </div>
                </article>
              );
            })}
          </section>
        )}
      </main>

      <SiteFooter />
    </div>
  );
}